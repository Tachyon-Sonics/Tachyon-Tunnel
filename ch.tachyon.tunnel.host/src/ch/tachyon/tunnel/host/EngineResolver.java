package ch.tachyon.tunnel.host;

class EngineResolver {
    
    private final static String ENGINE_CLASS = "ch.tachyon.tunnel.engine.Engine";
    
    private static IEngine engine;
    
    
    public static synchronized IEngine getEngine() {
        if (engine == null) {
            Class<?> engineClass;
            try {
                engineClass = Class.forName(ENGINE_CLASS); // [graal] should be ok if ch.tachyon.tunnel.engine project is in the classpath
                engine = (IEngine) engineClass.getConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(ex);
            } catch (SecurityException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return engine;
    }

}
