package com.leidos.glidepath.ead;

/**
 * Factory method to instantiate an appropriate ITrajectory concrete class
 *
 * User: ferenced
 * Date: 1/14/15
 * Time: 1:35 PM
 */
public class TrajectoryFactory {

    /**
     * Reflection helper to create a ITrajectory object based on class name
     *
     * @param trajectoryClassName
     * @return
     */
    public static ITrajectory newInstance(String trajectoryClassName)   {
        @SuppressWarnings("rawtypes")
		Class tClass = null;

        try   {
            tClass = Class.forName(trajectoryClassName);
        }
        catch(ClassNotFoundException cnfe)   {
            tClass = com.leidos.glidepath.ead.SimulatedTrajectory.class;
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

        return (ITrajectory) newObject;
    }

}
