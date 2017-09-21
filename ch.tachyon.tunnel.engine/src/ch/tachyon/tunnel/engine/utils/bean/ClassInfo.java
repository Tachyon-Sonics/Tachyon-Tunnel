package ch.tachyon.tunnel.engine.utils.bean;

import java.lang.reflect.*;
import java.util.*;

/**
 * The list of public properties of a given class.
 */
public class ClassInfo {

    private final Map<String, MemberInfo> members;


    public ClassInfo(Class<?> type) {
        this.members = new LinkedHashMap<String, MemberInfo>();
        for (Method method : type.getMethods()) {
            if (method.getReturnType() != null && !method.getReturnType().equals(Void.TYPE)
                    && method.getParameterTypes().length == 0 && !method.isSynthetic()
                    && ((method.getModifiers() & Modifier.STATIC) == 0)) {
                // Potential getter method found
                String readerName = method.getName();
                if (readerName.startsWith("is") || readerName.startsWith("get")) {
                    String propertyName;
                    if (readerName.startsWith("get"))
                        propertyName = readerName.substring("get".length());
                    else
                        propertyName = readerName.substring("is".length());
                    String writerName = "set" + propertyName;
                    try {
                        Method writer = type.getMethod(writerName, method.getReturnType());
                        if (writer != null && (writer.getReturnType() == null || writer.getReturnType().equals(Void.TYPE))
                                && !method.isSynthetic() && ((method.getModifiers() & Modifier.STATIC) == 0)) {
                            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                            // setter method found
                            MemberInfo member = new MemberInfo(propertyName, method, writer);
                            members.put(propertyName, member);
                        }
                    } catch (NoSuchMethodException ex) {
                        // No public writer. Continue
                    }
                }
            }
        }
    }

    public Collection<MemberInfo> getMembers() {
        return members.values();
    }

}
