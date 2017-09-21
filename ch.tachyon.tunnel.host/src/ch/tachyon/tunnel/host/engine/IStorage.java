package ch.tachyon.tunnel.host.engine;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import ch.tachyon.tunnel.host.effect.*;

public interface IStorage {

    public void storeParameterValues(Collection<IParameter> parameters, OutputStream output) throws IOException, InvocationTargetException;
    
    public void loadParameterValues(InputStream input, IPluginBundle bundle,
            Collection<IParameter> parameters) throws IOException, InvocationTargetException,
            IllegalAccessException, InstantiationException, ClassNotFoundException;
    
}