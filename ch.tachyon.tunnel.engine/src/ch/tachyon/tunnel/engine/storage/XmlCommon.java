package ch.tachyon.tunnel.engine.storage;


class XmlCommon {

    static final String ISNULL_ATTR = "isNull";
    static final String KEY_ATTR = "key";
    static final String REF_ATTR = "ref";
    static final String TYPE_ATTR = "type";
    static final String KEY_TYPE_ATTR = "keyType";
    static final String ITEM_TYPE_ATTR = "itemType";
    static final String KEY_NAME = "key";
    static final String ITEM_NAME = "item";


    public static boolean isSimple(Class<?> type) {
        return type.isPrimitive() || type.equals(String.class)
            || type.equals(Byte.class) || type.equals(Character.class)
            || type.equals(Short.class) || type.equals(Integer.class)
            || type.equals(Long.class) || type.equals(Float.class)
            || type.equals(Double.class) || type.equals(Boolean.class);
    }

}
