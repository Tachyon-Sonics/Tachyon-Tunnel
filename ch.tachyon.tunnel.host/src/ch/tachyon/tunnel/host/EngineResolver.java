package ch.tachyon.tunnel.host;

class EngineResolver {
    
    private final static String ENGINE_CLASS = "ch.tachyon.tunnel.engine.Engine";
    
    private static IEngine engine;
    
    
    public static synchronized IEngine getEngine() {
        if (engine == null) {
            Class<?> engineClass;
            try {
                engineClass = Class.forName(ENGINE_CLASS);
                engine = (IEngine) engineClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return engine;
    }

}
