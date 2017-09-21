package ch.tachyon.tunnel.engine.storage;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

import ch.tachyon.tunnel.engine.utils.bean.*;


public class XmlReader extends XmlCommon {

    private final ClassLoader classLoader;

    public XmlReader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public XmlReader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public Object fromXml(Document doc, String name) throws InvocationTargetException,
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        Element root = (Element)doc.getChildNodes().item(0);
        if (!root.getNodeName().equals(name))
            throw new IllegalArgumentException("Names do not match");
        String className = root.getAttribute(TYPE_ATTR);
        if (className == null)
            return null;
        Class<?> type = Class.forName(className, true, classLoader);
        return fromXml(type, root, new HashMap<String, Object>());
    }

    private Object fromXml(Class<?> type, Element elt, Map<String, Object> cache)
            throws InvocationTargetException, ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        if ("true".equalsIgnoreCase(elt.getAttribute(ISNULL_ATTR))) {
            return null;
        } else if (XmlCommon.isSimple(type)) {
            Text text = (Text)elt.getFirstChild();
            return parseSimpleType(type, text.getNodeValue());
        }
        String ref = elt.getAttribute(REF_ATTR);
        if (ref != null && !ref.equals("")) {
            if (!cache.containsKey(ref))
                throw new IllegalArgumentException("Forward reference: " + ref);
            return cache.get(ref);
        } else {
            Object result = null;
            if (Map.class.isAssignableFrom(type)) {
                result = mapFromXml(type, elt, cache);
            } else if (Collection.class.isAssignableFrom(type)) {
                result = collectionFromXml(type, elt, cache);
            } else if (Enum.class.isAssignableFrom(type)) {
                String name = (String) fromXml(String.class, elt, cache);
                for (Object item : type.getEnumConstants()) {
                    Enum<?> enumItem = (Enum<?>) item;
                    if (enumItem.name().equals(name))
                        result = item;
                }
            } else if (type.isArray()) {
                result = arrayFromXml(type, elt, cache);
            } else {
                result = type.newInstance();
                NodeList nodes = elt.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    if (node instanceof Element) {
                        Element child = (Element)node;
                        String name = child.getNodeName();
                        MemberInfo info = new MemberInfo(type, name);
                        Object value = fromXml(info.getType(), child, cache);
                        info.setValue(result, value);
                    }
                }
            }
            String key = elt.getAttribute(KEY_ATTR);
            if (key != null && !key.equals(""))
                cache.put(key, result);
            return result;
        }
    }

    private Object parseSimpleType(Class<?> type, String value) {
        if (type.equals(String.class)) {
            return (String)value;
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return Byte.valueOf(value);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return ((String)value).charAt(0);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return Short.valueOf(value);
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.valueOf(value);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.valueOf(value);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.valueOf(value);
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.valueOf(value);
        }
        return null;
    }

    private Map<?, ?> mapFromXml(Class<?> type, Element elt,
            Map<String, Object> cache) throws IllegalAccessException,
            InstantiationException, ClassNotFoundException, InvocationTargetException {
        Map<?, ?> result;
        if (type.isInterface()) {
            /*
             * Any Map implementation could be chosen. We choose
             * the one that preserves the order. If items are sorted
             * in the XML, they will at least stay sorted until the
             * map is modified.
             */
            // Hehe... this avoids "unchecked" warnings:
            result = LinkedHashMap.class.newInstance();
        } else {
            result = (Map<?,?>) type.newInstance();
        }
        String keyClassName = elt.getAttribute(KEY_TYPE_ATTR);
        Class<?> keysType = null;
        if (keyClassName != null && !keyClassName.equals(""))
            keysType = Class.forName(keyClassName, true, classLoader);
        String itemClassName = elt.getAttribute(ITEM_TYPE_ATTR);
        Class<?> itemsType = null;
        if (itemClassName != null && !itemClassName.equals(""))
            itemsType = Class.forName(itemClassName, true, classLoader);
        NodeList nodes = elt.getChildNodes();
        Object lastKey = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element)node;
                if (KEY_NAME.equals(child.getNodeName())) {
                    keyClassName = child.getAttribute(TYPE_ATTR);
                    Class<?> keyType = keysType;
                    if (keyClassName != null && !keyClassName.equals(""))
                        keyType = Class.forName(keyClassName, true, classLoader);
                    if (keyType == null)
                        throw new IllegalArgumentException("Unknown key type at " + elt);
                    lastKey = fromXml(keyType, child, cache);
                } else if (ITEM_NAME.equals(child.getNodeName())) {
                    itemClassName = child.getAttribute(TYPE_ATTR);
                    Class<?> itemType = itemsType;
                    if (itemClassName != null && !itemClassName.equals(""))
                        itemType = Class.forName(itemClassName, true, classLoader);
                    if (itemType == null)
                        throw new IllegalArgumentException("Unknown item type at " + elt);
                    Object item = fromXml(itemType, child, cache);
                    putUnsafe(result, lastKey, item);
                }
            }
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void putUnsafe(Map result, Object key, Object value) {
        result.put(key, value);
    }

    private Collection<?> collectionFromXml(Class<?> type, Element elt,
            Map<String, Object> cache) throws IllegalAccessException,
            InstantiationException, ClassNotFoundException, InvocationTargetException {
        Collection<?> result;
        if (type.isInterface()) {
            /*
             * Any collection implementation could be used.
             * We just choose the fastest one.
             */
            result = (Collection<?>) ArrayList.class.newInstance();
        } else {
            result = (Collection<?>) type.newInstance();
        }
        String itemClassName = elt.getAttribute(ITEM_TYPE_ATTR);
        Class<?> itemsType = null;
        if (itemClassName != null && !itemClassName.equals(""))
            itemsType = Class.forName(itemClassName, true, classLoader);
        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element)node;
                if (ITEM_NAME.equals(child.getNodeName())) {
                    itemClassName = child.getAttribute(TYPE_ATTR);
                    Class<?> itemType = itemsType;
                    if (itemClassName != null && !itemClassName.equals(""))
                        itemType = Class.forName(itemClassName, true, classLoader);
                    if (itemType == null)
                        throw new IllegalArgumentException("Unknown item type at " + elt);
                    Object item = fromXml(itemType, child, cache);
                    addUnsafe(result, item);
                }
            }
        }
        return result;
    }

    private Object[] arrayFromXml(Class<?> type, Element elt, Map<String, Object> cache) throws IllegalAccessException,
            InstantiationException, ClassNotFoundException, InvocationTargetException {
        List<?> result = new ArrayList<Object>();
        Class<?> itemsType = type.getComponentType();
        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element child = (Element)node;
                if (ITEM_NAME.equals(child.getNodeName())) {
                    String itemClassName = child.getAttribute(TYPE_ATTR);
                    Class<?> itemType = itemsType;
                    if (itemClassName != null && !itemClassName.equals(""))
                        itemType = Class.forName(itemClassName, true, classLoader);
                    if (itemType == null)
                        throw new IllegalArgumentException("Unknown item type at " + elt);
                    if (!itemsType.isAssignableFrom(itemType))
                        throw new IllegalArgumentException("Item type " + itemType + " not assignable to array component type " + itemsType);
                    Object item = fromXml(itemType, child, cache);
                    addUnsafe(result, item);
                }
            }
        }
        return result.toArray();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addUnsafe(Collection coll, Object item) {
        coll.add(item);
    }

    public Object loadXml(Reader reader, String name) throws
            InvocationTargetException, IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        Source src = new StreamSource(reader);
        DOMResult dst = new DOMResult();
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.transform(src, dst);
        } catch (TransformerException ex) {
            throw new UnsupportedOperationException(ex);
        }
        Document doc = (Document)dst.getNode();
        return fromXml(doc, name);
    }

    public Object loadXml(File file, String name) throws IOException,
            InvocationTargetException, IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        FileReader freader = new FileReader(file);
        BufferedReader breader = new BufferedReader(freader);
        try {
            return loadXml(breader, name);
        } finally {
            breader.close();
        }
    }

}
