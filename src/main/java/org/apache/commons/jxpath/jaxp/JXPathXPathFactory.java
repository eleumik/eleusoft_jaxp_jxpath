package org.apache.commons.jxpath.jaxp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.ExpressionContext;
import org.apache.commons.jxpath.Function;
import org.apache.commons.jxpath.Functions;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.jxpath.NodeSet;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.Variables;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link XPathFactory}
 * based on JXPath.
 * <p>In order to install this factory, follow
 * the instructions in {@link XPathFactory}
 * documentation.
 * <p>The easiest option is to set a system property
 * with something like:
 * <p><pre>
 * -Djavax.xml.xpath.XPathFactory:http://commons.apache.org/jxpath=org.apache.commons.jxpath.jaxp.JXPathXPathFactory
 * </pre>
 * <p>
 * @author Michele Vivoda
 */
public class JXPathXPathFactory extends XPathFactory
{
    /**
     * URI of the JXPath object model.
     */
    public static final String URI =
        "http://commons.apache.org/jxpath";
    /**
     * Per - factory resolver.
     */
    private XPathVariableResolver variableResolver;
    /**
     * Per - factory resolver.
     */
    private XPathFunctionResolver functionResolver;
    /**
     * Whether secure processing is on.
     */
    private boolean secure;
    /*
     * (non-Javadoc)
     * @see javax.xml.xpath.XPathFactory#getFeature(java.lang.String)
     */
    public boolean getFeature(final String name) throws XPathFactoryConfigurationException
    {
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING))
        {
            return this.secure;
        }
        else throw new  XPathFactoryConfigurationException("Unsupported feature:" + name);
    }
    /*
     * (non-Javadoc)
     * @see javax.xml.xpath.XPathFactory#setFeature(java.lang.String, boolean)
     */
    public void setFeature(final String name, final boolean arg1) throws XPathFactoryConfigurationException
    {
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING))
        {
            this.secure = arg1;
        }
        else throw new XPathFactoryConfigurationException("Unsupported feature:" + name);
    }
    /*
     * (non-Javadoc)
     * @see javax.xml.xpath.XPathFactory#isObjectModelSupported(java.lang.String)
     */
    public boolean isObjectModelSupported(final String uri)
    {
        return uri.equals(URI) ||
            uri.equals(XPathFactory.DEFAULT_OBJECT_MODEL_URI);
    }
    /*
     * (non-Javadoc)
     * @see javax.xml.xpath.XPathFactory#newXPath()
     */
    public XPath newXPath()
    {
        return new XPathImpl(variableResolver, functionResolver, secure);
    }
    /*
     * (non-Javadoc)
     * @see javax.xml.xpath.XPathFactory#setXPathFunctionResolver(javax.xml.xpath.XPathFunctionResolver)
     */
    public void setXPathFunctionResolver(final XPathFunctionResolver obj)
    {
        this.functionResolver = obj;
    }
    /*
     * (non-Javadoc)
     * @see javax.xml.xpath.XPathFactory#setXPathVariableResolver(javax.xml.xpath.XPathVariableResolver)
     */
    public void setXPathVariableResolver(final XPathVariableResolver obj)
    {
        this.variableResolver = obj;
    }
    /**
     * Baseclass for {@link XPathImpl} and {@link XPathExpressionImpl}.
     */
    private static class ResolversSupport
    {
        protected XPathVariableResolver variableResolver;
        protected XPathFunctionResolver functionResolver;
        protected NamespaceContext nsContext;
        protected final boolean secure;

        /**
         * Constructor for subclasses.
         * @param vr optional {@link XPathVariableResolver}
         * @param fr optional {@link XPathFunctionResolver}
         * @param secure secure mode flag.
         */
        protected ResolversSupport(final XPathVariableResolver vr,
                                   final XPathFunctionResolver fr,
                                   final boolean secure)
        {
            this.variableResolver = vr;
            this.functionResolver = fr;
            this.secure = secure;
        }
        /**
         * Returns a new, configured, instance of JXPathContext
         * @param obj the value
         * @return a JXPathContext, never null.
         */
        protected JXPathContext newContext(final Object obj)
        {
            final JXPathContext ctx = JXPathContext.newContext(obj);
            ctx.setLenient(true);
            // Functions
            if (secure)
            {
                ctx.setFunctions(SECUREFUNCTIONS);
            }
            else if (functionResolver!=null)
            {
                ctx.setFunctions(new FunctionsImpl(functionResolver, nsContext));
            }
            // Variables
            if (variableResolver!=null)
            {
                ctx.setVariables(new VariablesImpl(variableResolver, nsContext));
            }
            return ctx;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#getNamespaceContext()
         */
        public NamespaceContext getNamespaceContext()
        {
            return nsContext;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#getXPathFunctionResolver()
         */
        public XPathFunctionResolver getXPathFunctionResolver()
        {
            return functionResolver;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#getXPathVariableResolver()
         */
        public XPathVariableResolver getXPathVariableResolver()
        {
            return variableResolver;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#setNamespaceContext(javax.xml.namespace.NamespaceContext)
         */
        public void setNamespaceContext(final NamespaceContext ns)
        {
            if (ns==null) throw new NullPointerException("Null namespace context");
            this.nsContext = ns;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#setXPathFunctionResolver(javax.xml.xpath.XPathFunctionResolver)
         */
        public void setXPathFunctionResolver(final XPathFunctionResolver resolver)
        {
            if (resolver==null) throw new NullPointerException("Null function resolver");
            this.functionResolver = resolver;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#setXPathVariableResolver(javax.xml.xpath.XPathVariableResolver)
         */
        public void setXPathVariableResolver(final XPathVariableResolver resolver)
        {
            if (resolver==null) throw new NullPointerException("Null variable resolver");
            this.variableResolver= resolver;

        }

        private static class VariablesImpl implements Variables
        {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            final XPathVariableResolver variableResolver;
            //final NamespaceContext nsContext;
            VariablesImpl(final XPathVariableResolver resolver, final NamespaceContext ns)
            {
                this.variableResolver = resolver;
                //this.nsContext = ns;
            }

            public void declareVariable(String varName, Object value)
            {
                throw new RuntimeException("Unsupported");
            }

            public Object getVariable(String varName)
            {
                // TODO issue: JXPath does not use qnames for variables ?
                return variableResolver.resolveVariable(new QName(XMLConstants.NULL_NS_URI, varName));
            }

            public boolean isDeclaredVariable(String varName)
            {
                return getVariable(varName)!=null;
            }

            public void undeclareVariable(String varName)
            {
                throw new RuntimeException("Unsupported");
            }

        }
        /**
         * Singleton instance of SecureFunctions
         */
        static Functions SECUREFUNCTIONS = new SecureFunctions();

        private static class SecureFunctions implements Functions, Function
        {

            public Function getFunction(String namespace,
                                        String name,
                                        Object[] parameters)
            {
                return this;
            }

            public Set getUsedNamespaces()
            {
                return Collections.singleton("");
            }
            /**
             *
             * @throws always throws SecureFunctionException
             */
            public Object invoke(ExpressionContext context, Object[] parameters)
            //throws XPathFunctionException
            {

                //throw new XPathFunctionException("Secure mode is on, cannot use functions.");
                throw new SecureFunctionException("Secure mode is on, cannot use functions.");
            }

        }
        /**
         * Runtime exception thrown by {@link SecureFunctions},
         * will be converted to a {@link XPathFunctionException}.
         *
         */
        static class SecureFunctionException extends RuntimeException
        {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            SecureFunctionException(final String msg)
            {
                super(msg);
            }
        }
        private static class FunctionsImpl implements Functions
        {
            final XPathFunctionResolver functionResolver;
            final NamespaceContext nsContext;
            FunctionsImpl(final XPathFunctionResolver resolver, final NamespaceContext ns)
            {
                this.functionResolver = resolver;
                this.nsContext = ns;
            }
            public Function getFunction(String namespace,
                                        String name,
                                        Object[] parameters)
            {
                // TODO JXPath uses prefixes as uris ?
                if (nsContext!=null) namespace = nsContext.getNamespaceURI(namespace);
                //System.out.println("getFunction(" + namespace + ", " + name + ")");
                final XPathFunction function = functionResolver.resolveFunction(
                    new QName(namespace, name), parameters.length);
                return function==null ? null : new FunctionImpl(function);
            }

            public Set getUsedNamespaces()
            {
                return Collections.singleton("");
            }

            private static class FunctionImpl implements Function
            {
                private final XPathFunction function;

                private FunctionImpl(XPathFunction f)
                {
                    this.function = f;
                }


                public Object invoke(ExpressionContext context,
                                     Object[] parameters)
                {
                    final ArrayList list = new ArrayList();
                    for(int i=0, len=parameters.length;i<len;i++)
                    {
                        final Object jxpathParam = parameters[i];
                        final Object xpathParam;
                        if (jxpathParam instanceof NodeSet)
                        {
                            xpathParam = new NodeListImpl( ((NodeSet)jxpathParam).getNodes());
                        }
                        else xpathParam = jxpathParam;
                        list.add(xpathParam);
                    }
                    try
                    {
                        return function.evaluate(list);
                    }
                    catch(XPathFunctionException e)
                    {
                        throw new JXPathException("Function error:" + e.getMessage(), e);
                    }
                }

            }
        }

    }


    /**
     * Implementation of XPath.
     */
    private final static class XPathImpl extends ResolversSupport implements XPath
    {

        private final XPathVariableResolver origVariableResolver;
        private final XPathFunctionResolver origFunctionResolver;

        XPathImpl (final XPathVariableResolver vr,
            final XPathFunctionResolver fr,
            final boolean secure)
        {
            super(vr, fr, secure);
            this.origVariableResolver = vr;
            this.origFunctionResolver = fr;

        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#compile(java.lang.String)
         */
        public XPathExpression compile(final String expression) throws XPathExpressionException
        {
            try
            {
                final CompiledExpression ce = JXPathContext.compile(expression);
                return new XPathExpressionImpl(ce, expression, this.functionResolver, this.variableResolver, nsContext, this.secure);
            }
            catch(JXPathException e)
            {
                throw new XPathExpressionException(e);
            }
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#evaluate(java.lang.String, java.lang.Object)
         */
        public String evaluate(final String expression, final Object item) throws XPathExpressionException
        {
            return (String) evaluate(expression, item, XPathConstants.STRING);
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#evaluate(java.lang.String, org.xml.sax.InputSource)
         */
        public String evaluate(final String expression, final InputSource source) throws XPathExpressionException
        {
             return (String)evaluate(expression, source, XPathConstants.STRING);
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#evaluate(java.lang.String, java.lang.Object, javax.xml.namespace.QName)
         */
        public Object evaluate(final String expression, final Object item, final QName returnType) throws XPathExpressionException
        {
            final JXPathContext jxPathContext = newContext(item);
            try
            {
                if (XPathConstants.NODE.equals(returnType))
                {
                    return jxPathContext.selectSingleNode(expression);
                }
                else if (XPathConstants.NODESET.equals(returnType))
                {
                    return new NodeListImpl(jxPathContext.selectNodes(expression));
                }
                else
                {
                    final Object value = jxPathContext.selectSingleNode(expression);
                    return convertValue(value, returnType);
                }
            }
            catch(SecureFunctionException e) {
                throw new XPathFunctionException(e);
            }
            catch (JXPathException e) {
                throw new XPathExpressionException(e);
            }
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#evaluate(java.lang.String, org.xml.sax.InputSource, javax.xml.namespace.QName)
         */
        public Object evaluate(final String expression,
                               final InputSource source,
                               final QName returnType) throws XPathExpressionException
        {
            return evaluate(expression, getDocument(source, this.secure), returnType);
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPath#reset()
         */
        public void reset()
        {
            // Javadoc: reset()
            // XPath is reset to the same state as when
            // it was created with XPathFactory.newXPath().
            // reset() is designed to allow the reuse of
            // existing XPaths thus saving resources associated
            // with the creation of new XPaths.
            // The reset XPath is not guaranteed to have the
            // same XPathFunctionResolver, XPathVariableResolver
            // or NamespaceContext Objects, e.g. Object.equals(Object obj).
            // It is guaranteed to have a functionally equal
            // XPathFunctionResolver, XPathVariableResolver and NamespaceContext.
            this.variableResolver = this.origVariableResolver;
            this.functionResolver = this.origFunctionResolver;
            this.nsContext = null;



        }


    }
    /**
     * Implementation of a compiled expression.
     */
    private static class XPathExpressionImpl extends ResolversSupport implements XPathExpression
    {
        private final CompiledExpression compiled;
        private final String xpathString;
        XPathExpressionImpl(final CompiledExpression ce,
            final String xpath,
            final XPathFunctionResolver fr,
            final XPathVariableResolver vr,
            final NamespaceContext nsContext,
            final boolean secure)
        {
            super(vr, fr, secure);
            this.compiled = ce;
            this.nsContext = nsContext;
            this.xpathString = xpath;
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPathExpression#evaluate(java.lang.Object)
         */
        public String evaluate(final Object obj) throws XPathExpressionException
        {
            return (String)evaluate(obj, XPathConstants.STRING);
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPathExpression#evaluate(org.xml.sax.InputSource)
         */
        public String evaluate(final InputSource inputsource) throws XPathExpressionException
        {
            return (String)evaluate(inputsource, XPathConstants.STRING);
        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPathExpression#evaluate(java.lang.Object, javax.xml.namespace.QName)
         */
        public Object evaluate(final Object obj, final QName returnType) throws XPathExpressionException
        {
            if (obj==null) throw new NullPointerException("Null object");

            final JXPathContext jxPathContext = newContext(obj);
            try
            {
                if (XPathConstants.NODE.equals(returnType))
                {
                    // TODO this is an inconsistency in JXPath api,
                    // must pass xpath, that is used only for errors..
                    final Pointer p = compiled.getPointer(jxPathContext, xpathString);
                    return p==null ? null : p.getNode();
                }
                else if (XPathConstants.NODESET.equals(returnType))
                {
                    // TODO here there is a difference because
                    // selectNodes does not exist on CompiledExpression
                    final ArrayList list = new ArrayList();
                    final Iterator iter = compiled.iteratePointers(jxPathContext);
                    while(iter.hasNext())
                    {
                        list.add(((Pointer)iter.next()).getNode());
                    }
                    return new NodeListImpl(list);
                }
                else
                {
                    final Pointer p = compiled.getPointer(jxPathContext, xpathString);
                    return convertValue(p==null ? null : p.getNode(),
                        returnType);
                }

            }
            catch(SecureFunctionException e) {
                throw new XPathFunctionException(e);
            }
            catch (JXPathException e) {
                throw new XPathExpressionException(e);
            }

        }
        /*
         * (non-Javadoc)
         * @see javax.xml.xpath.XPathExpression#evaluate(org.xml.sax.InputSource, javax.xml.namespace.QName)
         */
        public Object evaluate(final InputSource inputsource, final QName qname) throws XPathExpressionException
        {
            return evaluate(getDocument(inputsource, this.secure), qname);
        }

    }

    /**
     * Helper, converts a return value from
     * {@link JXPathContext#getValue(String)}
     * to the requested return type.
     * Package protected because used by inner classes.
     * @param value
     * @param returnType
     * @return
     */
    static Object convertValue(final Object value, final QName returnType)
    throws XPathExpressionException
    {
        return XPathValues.convertValue(value, returnType);
    }

    /**
     * Retrieves a DOM document from an input source.
     * <p>Method is package protected for performance,
     * to avoid creation of special accessor when
     * accessing from inner classes.
     * @param is a required input source.
     * @return a Document, never null.
     * @throws XPathExpressionException when fails.
     */
    static Document getDocument(final InputSource is, boolean secure)
        throws XPathExpressionException
    {
        final DocumentBuilder builder = getDocumentBuilder(secure);
        try
        {
            return builder.parse(is);
        }
        catch(SAXException ioe)
        {
            // Also xalan uses XPathExpressionException for these errors
            throw new XPathExpressionException(ioe);
        }
        catch(IOException ioe)
        {
            // Also xalan uses XPathExpressionException for these errors
            throw new XPathExpressionException(ioe);
        }
    }

    private static DocumentBuilder getDocumentBuilder(boolean secure)
    {
        try
        {
            // This is a BIG performance hit..
            // but it looks like is inevitable,
            // see also org.apache.xpath.jaxp.XPathImpl#getParser()
            // code-documentation for the same issue.
            // http://svn.apache.org/repos/asf/xalan/java/trunk/src/org/apache/xpath/jaxp/XPathImpl.java
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            // Very important..otherwise fails in counting text() nodes,
            // considering a CData and an adjacent text as two distinct nodes
            factory.setCoalescing(true);
            factory.setExpandEntityReferences(!secure);
            return factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new Error("JAXP config error:" + e.getMessage(), e);
        }

    }

}
