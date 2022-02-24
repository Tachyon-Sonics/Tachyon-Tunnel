package ch.tachyon.tunnel.engine.storage;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

import ch.tachyon.tunnel.engine.utils.bean.*;


public class XmlWriter extends XmlCommon {

    public XmlWriter() {

    }

    public Document toXml(Object obj, String name) throws InvocationTargetException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement(name);
            Class<?> type = (obj == null ? null : obj.getClass());
            if (type != null)
                root.setAttribute(TYPE_ATTR, type.getName());
            else
                root.setAttribute(ISNULL_ATTR, "true");
            document.appendChild(root);
            RunningCache cache = new RunningCache();
            // Create document
            toXml(document, root, type, obj, cache);
            // Add ids to referenced elements
            for (Object item : cache.getReferencedObjects()) {
                Element node = cache.getNode(item);
                long key = cache.getKey(item);
                node.setAttribute(KEY_ATTR, Long.toString(key));
            }
            return document;
        } catch (ParserConfigurationException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    private void toXml(Document document, Element elt, Class<?> type, Object value, RunningCache cache) throws InvocationTargetException {
        if (value == null) {
            elt.setAttribute(ISNULL_ATTR, "true");
            return;
        } if (XmlCommon.isSimple(type)) {
            Text text = document.createTextNode(value.toString());
            elt.appendChild(text);
            return;
        } else if (cache.isDone(value)) {
            long key = cache.getKey(value);
            elt.setAttribute(REF_ATTR, Long.toString(key));
            return;
        }

        cache.add(value, elt);
        if (Map.class.isAssignableFrom(type)) {
            mapToXml(document, elt, value, cache);
        } else if (Collection.class.isAssignableFrom(type)) {
            collectionToXml(document, elt, value, cache);
        } else if (type.isArray()) {
            arrayToXml(document, elt, (Object[]) value, cache);
        } else if (Enum.class.isAssignableFrom(type)) {
            Enum<?> enumVal = (Enum<?>) value;
            toXml(document, elt, String.class, enumVal.name(), cache);
        } else {
            ClassInfo info = new ClassInfo(type);
            for (MemberInfo childMember : info.getMembers()) {
                Element child = document.createElement(childMember.getName());
                elt.appendChild(child);
                Class<?> childType = childMember.getType();
                Object childValue = childMember.getValue(value);
                toXml(document, child, childType, childValue, cache);
            }
        }
    }

    private void mapToXml(Document document, Element elt, Object value, RunningCache cache) throws InvocationTargetException {
        Map<?,?> map = (Map<?,?>)value;
        Class<?> keysType = getCommonClass(map.keySet());
        if (keysType != null)
            elt.setAttribute(KEY_TYPE_ATTR, keysType.getName());
        Class<?> itemsType = getCommonClass(map.values());
        if (itemsType != null)
            elt.setAttribute(ITEM_TYPE_ATTR, itemsType.getName());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Element keyChild = document.createElement(KEY_NAME);
            elt.appendChild(keyChild);
            Object key = entry.getKey();
            Class<?> keyType = (key == null ? null : key.getClass());
            if (keysType == null && keyType != null)
                keyChild.setAttribute(TYPE_ATTR, keyType.getName());
            toXml(document, keyChild, keyType, key, cache);

            Element itemChild = document.createElement(ITEM_NAME);
            elt.appendChild(itemChild);
            Object item = entry.getValue();
            Class<?> itemType = (item == null ? null : item.getClass());
            if (itemType != null && itemType.getSuperclass().isEnum())
                itemType = itemType.getSuperclass(); // Replace enum item's type by enum's type
            if (itemsType == null && itemType != null)
                itemChild.setAttribute(TYPE_ATTR, itemType.getName());
            toXml(document, itemChild, itemType, item, cache);
        }
    }

    private void collectionToXml(Document document, Element elt, Object value, RunningCache cache) throws InvocationTargetException {
        Collection<?> coll = (Collection<?>)value;
        Class<?> itemsType = getCommonClass(coll);
        if (itemsType != null)
            elt.setAttribute(ITEM_TYPE_ATTR, itemsType.getName());
        for (Object item : coll) {
            Element child = document.createElement(ITEM_NAME);
            elt.appendChild(child);
            Class<?> itemType = (item == null ? null : item.getClass());
            if (itemsType == null && itemType != null)
                child.setAttribute(TYPE_ATTR, itemType.getName());
            toXml(document, child, itemType, item, cache);
        }
    }

    private void arrayToXml(Document document, Element elt, Object[] array, RunningCache cache) throws InvocationTargetException {
        Class<?> itemsType = getCommonClass(Arrays.asList(array));
        if (itemsType != null)
            elt.setAttribute(ITEM_TYPE_ATTR, itemsType.getName());
        for (Object item : array) {
            Element child = document.createElement(ITEM_NAME);
            elt.appendChild(child);
            Class<?> itemType = (item == null ? null : item.getClass());
            if (itemsType == null && itemType != null)
                child.setAttribute(TYPE_ATTR, itemType.getName());
            toXml(document, child, itemType, item, cache);
        }
    }

    private static Class<?> getCommonClass(Collection<?> coll) {
        Class<?> result = null;
        for (Object item : coll) {
            if (item != null) {
                Class<?> type = item.getClass();
                if (result == null) {
                    result = type;
                } else if (!result.equals(type)) {
                    return null; // Not all items have the same type
                }
            }
        }
        return result;
    }

    public void saveXml(Object obj, String name, Writer writer) throws IOException, InvocationTargetException {
        Document doc = toXml(obj, name);
        Source src = new DOMSource(doc);
        Result dst = new StreamResult(writer);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute("indent-number", 2);
            Transformer trans = factory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(src, dst);
        } catch (TransformerException ex) {
            throw new UnsupportedOperationException(ex);
        }
        writer.flush();

//        DOMImplementation domImpl = doc.getImplementation();
//        DOMImplementationLS domImplLS = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");
//        LSSerializer serializer = domImplLS.createLSSerializer();
//        DOMConfiguration config = serializer.getDomConfig();
//        config.setParameter("format-pretty-print", true);
//        LSOutput output = domImplLS.createLSOutput();
//        output.setCharacterStream(writer);
//        serializer.write(doc, output);
//        writer.flush();
    }

    public void saveXml(Object obj, String name, File file)  throws IOException, InvocationTargetException {
        Writer fwriter = new FileWriter(file, false);
        BufferedWriter bwriter = new BufferedWriter(fwriter);
        try {
            saveXml(obj, name, bwriter);
        } finally {
            bwriter.close();
        }
    }

}
