package com.leidos.glidepath.ead;

/**
 * Creates a concrete IEad class based on provided class name.
 * 
 * @author starkj
 *
 */
public class EadFactory {
	
	public static IEad newInstance(String className) {
		@SuppressWarnings("rawtypes")
		Class tClass = null;
		
        try   {
            tClass = Class.forName(className);
        }
        catch(ClassNotFoundException cnfe)   {
            tClass = com.leidos.glidepath.ead.EadSimple.class;
        }

        Object newObject = null;
        try   {
            newObject = tClass.newInstance();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return (IEad)newObject;
	}
	
}
