
package com.nineoldandroids.animation;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.nineoldandroids.util.FloatProperty;
import com.nineoldandroids.util.IntProperty;
import com.nineoldandroids.util.Property;

public class PropertyValuesHolder implements Cloneable {

    String mPropertyName;

    /**
     * @hide
     */
    protected Property mProperty;

    Method mSetter = null;

    private Method mGetter = null;

    /**
     * The type of values supplied. This information is used both in deriving the setter/getter
     * functions and in deriving the type of TypeEvaluator.
     */
    Class mValueType;

    /**
     * The set of keyframes (time/value pairs) that define this animation.
     */
    KeyframeSet mKeyframeSet = null;


    // type evaluators for the primitive types handled by this implementation
    private static final TypeEvaluator sIntEvaluator = new IntEvaluator();
    private static final TypeEvaluator sFloatEvaluator = new FloatEvaluator();

    private static Class[] FLOAT_VARIANTS = {float.class, Float.class, double.class, int.class,
            Double.class, Integer.class};
    private static Class[] INTEGER_VARIANTS = {int.class, Integer.class, float.class, double.class,
            Float.class, Double.class};
    private static Class[] DOUBLE_VARIANTS = {double.class, Double.class, float.class, int.class,
            Float.class, Integer.class};

    // These maps hold all property entries for a particular class. This map
    // is used to speed up property/setter/getter lookups for a given class/property
    // combination. No need to use reflection on the combination more than once.
    private static final HashMap<Class, HashMap<String, Method>> sSetterPropertyMap =
            new HashMap<Class, HashMap<String, Method>>();
    private static final HashMap<Class, HashMap<String, Method>> sGetterPropertyMap =
            new HashMap<Class, HashMap<String, Method>>();

    // This lock is used to ensure that only one thread is accessing the property maps
    // at a time.
    final ReentrantReadWriteLock mPropertyMapLock = new ReentrantReadWriteLock();

    // Used to pass single value to varargs parameter in setter invocation
    final Object[] mTmpValueArray = new Object[1];

    /**
     * The type evaluator used to calculate the animated values. This evaluator is determined
     * automatically based on the type of the start/end objects passed into the constructor,
     * but the system only knows about the primitive types int and float. Any other
     * type will need to set the evaluator to a custom evaluator for that type.
     */
    private TypeEvaluator mEvaluator;

    private Object mAnimatedValue;

    /**
     * Internal utility constructor, used by the factory methods to set the property name.
     * @param propertyName The name of the property for this holder.
     */
    private PropertyValuesHolder(String propertyName) {
        mPropertyName = propertyName;
    }

    /**
     * Internal utility constructor, used by the factory methods to set the property.
     * @param property The property for this holder.
     */
    private PropertyValuesHolder(Property property) {
        mProperty = property;
        if (property != null) {
            mPropertyName = property.getName();
        }
    }

    public static PropertyValuesHolder ofInt(String propertyName, int... values) {
        return new IntPropertyValuesHolder(propertyName, values);
    }

    public static PropertyValuesHolder ofInt(Property<?, Integer> property, int... values) {
        return new IntPropertyValuesHolder(property, values);
    }

    public static PropertyValuesHolder ofFloat(String propertyName, float... values) {
        return new FloatPropertyValuesHolder(propertyName, values);
    }

    public static PropertyValuesHolder ofFloat(Property<?, Float> property, float... values) {
        return new FloatPropertyValuesHolder(property, values);
    }

    public static PropertyValuesHolder ofObject(String propertyName, TypeEvaluator evaluator,
            Object... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder(propertyName);
        pvh.setObjectValues(values);
        pvh.setEvaluator(evaluator);
        return pvh;
    }

    public static <V> PropertyValuesHolder ofObject(Property property,
            TypeEvaluator<V> evaluator, V... values) {
        PropertyValuesHolder pvh = new PropertyValuesHolder(property);
        pvh.setObjectValues(values);
        pvh.setEvaluator(evaluator);
        return pvh;
    }

    public static PropertyValuesHolder ofKeyframe(String propertyName, Keyframe... values) {
        KeyframeSet keyframeSet = KeyframeSet.ofKeyframe(values);
        if (keyframeSet instanceof IntKeyframeSet) {
            return new IntPropertyValuesHolder(propertyName, (IntKeyframeSet) keyframeSet);
        } else if (keyframeSet instanceof FloatKeyframeSet) {
            return new FloatPropertyValuesHolder(propertyName, (FloatKeyframeSet) keyframeSet);
        }
        else {
            PropertyValuesHolder pvh = new PropertyValuesHolder(propertyName);
            pvh.mKeyframeSet = keyframeSet;
            pvh.mValueType = ((Keyframe)values[0]).getType();
            return pvh;
        }
    }

    public static PropertyValuesHolder ofKeyframe(Property property, Keyframe... values) {
        KeyframeSet keyframeSet = KeyframeSet.ofKeyframe(values);
        if (keyframeSet instanceof IntKeyframeSet) {
            return new IntPropertyValuesHolder(property, (IntKeyframeSet) keyframeSet);
        } else if (keyframeSet instanceof FloatKeyframeSet) {
            return new FloatPropertyValuesHolder(property, (FloatKeyframeSet) keyframeSet);
        }
        else {
            PropertyValuesHolder pvh = new PropertyValuesHolder(property);
            pvh.mKeyframeSet = keyframeSet;
            pvh.mValueType = ((Keyframe)values[0]).getType();
            return pvh;
        }
    }

    public void setIntValues(int... values) {
        mValueType = int.class;
        mKeyframeSet = KeyframeSet.ofInt(values);
    }

    public void setFloatValues(float... values) {
        mValueType = float.class;
        mKeyframeSet = KeyframeSet.ofFloat(values);
    }

    /**
     * Set the animated values for this object to this set of Keyframes.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setKeyframes(Keyframe... values) {
        int numKeyframes = values.length;
        Keyframe keyframes[] = new Keyframe[Math.max(numKeyframes,2)];
        mValueType = ((Keyframe)values[0]).getType();
        for (int i = 0; i < numKeyframes; ++i) {
            keyframes[i] = (Keyframe)values[i];
        }
        mKeyframeSet = new KeyframeSet(keyframes);
    }

    public void setObjectValues(Object... values) {
        mValueType = values[0].getClass();
        mKeyframeSet = KeyframeSet.ofObject(values);
    }

    private Method getPropertyFunction(Class targetClass, String prefix, Class valueType) {
        // TODO: faster implementation...
        Method returnVal = null;
        String methodName = getMethodName(prefix, mPropertyName);
        Class args[] = null;
        if (valueType == null) {
            try {
                returnVal = targetClass.getMethod(methodName, args);
            } catch (NoSuchMethodException e) {
                /* The native implementation uses JNI to do reflection, which allows access to private methods.
                 * getDeclaredMethod(..) does not find superclass methods, so it's implemented as a fallback.
                 */
                try {
                    returnVal = targetClass.getDeclaredMethod(methodName, args);
                    returnVal.setAccessible(true);
                } catch (NoSuchMethodException e2) {
                    Log.e("PropertyValuesHolder",
                            "Couldn't find no-arg method for property " + mPropertyName + ": " + e);
                }
            }
        } else {
            args = new Class[1];
            Class typeVariants[];
            if (mValueType.equals(Float.class)) {
                typeVariants = FLOAT_VARIANTS;
            } else if (mValueType.equals(Integer.class)) {
                typeVariants = INTEGER_VARIANTS;
            } else if (mValueType.equals(Double.class)) {
                typeVariants = DOUBLE_VARIANTS;
            } else {
                typeVariants = new Class[1];
                typeVariants[0] = mValueType;
            }
            for (Class typeVariant : typeVariants) {
                args[0] = typeVariant;
                try {
                    returnVal = targetClass.getMethod(methodName, args);
                    // change the value type to suit
                    mValueType = typeVariant;
                    return returnVal;
                } catch (NoSuchMethodException e) {
                    /* The native implementation uses JNI to do reflection, which allows access to private methods.
                     * getDeclaredMethod(..) does not find superclass methods, so it's implemented as a fallback.
                     */
                    try {
                        returnVal = targetClass.getDeclaredMethod(methodName, args);
                        returnVal.setAccessible(true);
                        // change the value type to suit
                        mValueType = typeVariant;
                        return returnVal;
                    } catch (NoSuchMethodException e2) {
                        // Swallow the error and keep trying other variants
                    }
                }
            }
            // If we got here, then no appropriate function was found
            Log.e("PropertyValuesHolder",
                    "Couldn't find setter/getter for property " + mPropertyName +
                            " with value type "+ mValueType);
        }

        return returnVal;
    }


    /**
     * Returns the setter or getter requested. This utility function checks whether the
     * requested method exists in the propertyMapMap cache. If not, it calls another
     * utility function to request the Method from the targetClass directly.
     * @param targetClass The Class on which the requested method should exist.
     * @param propertyMapMap The cache of setters/getters derived so far.
     * @param prefix "set" or "get", for the setter or getter.
     * @param valueType The type of parameter passed into the method (null for getter).
     * @return Method the method associated with mPropertyName.
     */
    private Method setupSetterOrGetter(Class targetClass,
            HashMap<Class, HashMap<String, Method>> propertyMapMap,
            String prefix, Class valueType) {
        Method setterOrGetter = null;
        try {
            // Have to lock property map prior to reading it, to guard against
            // another thread putting something in there after we've checked it
            // but before we've added an entry to it
            mPropertyMapLock.writeLock().lock();
            HashMap<String, Method> propertyMap = propertyMapMap.get(targetClass);
            if (propertyMap != null) {
                setterOrGetter = propertyMap.get(mPropertyName);
            }
            if (setterOrGetter == null) {
                setterOrGetter = getPropertyFunction(targetClass, prefix, valueType);
                if (propertyMap == null) {
                    propertyMap = new HashMap<String, Method>();
                    propertyMapMap.put(targetClass, propertyMap);
                }
                propertyMap.put(mPropertyName, setterOrGetter);
            }
        } finally {
            mPropertyMapLock.writeLock().unlock();
        }
        return setterOrGetter;
    }

    /**
     * Utility function to get the setter from targetClass
     * @param targetClass The Class on which the requested method should exist.
     */
    void setupSetter(Class targetClass) {
        mSetter = setupSetterOrGetter(targetClass, sSetterPropertyMap, "set", mValueType);
    }

    /**
     * Utility function to get the getter from targetClass
     */
    private void setupGetter(Class targetClass) {
        mGetter = setupSetterOrGetter(targetClass, sGetterPropertyMap, "get", null);
    }

    void setupSetterAndGetter(Object target) {
        if (mProperty != null) {
            // check to make sure that mProperty is on the class of target
            try {
                Object testValue = mProperty.get(target);
                for (Keyframe kf : mKeyframeSet.mKeyframes) {
                    if (!kf.hasValue()) {
                        kf.setValue(mProperty.get(target));
                    }
                }
                return;
            } catch (ClassCastException e) {
                Log.e("PropertyValuesHolder","No such property (" + mProperty.getName() +
                        ") on target object " + target + ". Trying reflection instead");
                mProperty = null;
            }
        }
        Class targetClass = target.getClass();
        if (mSetter == null) {
            setupSetter(targetClass);
        }
        for (Keyframe kf : mKeyframeSet.mKeyframes) {
            if (!kf.hasValue()) {
                if (mGetter == null) {
                    setupGetter(targetClass);
                }
                try {
                    kf.setValue(mGetter.invoke(target));
                } catch (InvocationTargetException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                } catch (IllegalAccessException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                }
            }
        }
    }

    /**
     * Utility function to set the value stored in a particular Keyframe. The value used is
     * whatever the value is for the property name specified in the keyframe on the target object.
     *
     * @param target The target object from which the current value should be extracted.
     * @param kf The keyframe which holds the property name and value.
     */
    private void setupValue(Object target, Keyframe kf) {
        if (mProperty != null) {
            kf.setValue(mProperty.get(target));
        }
        try {
            if (mGetter == null) {
                Class targetClass = target.getClass();
                setupGetter(targetClass);
            }
            kf.setValue(mGetter.invoke(target));
        } catch (InvocationTargetException e) {
            Log.e("PropertyValuesHolder", e.toString());
        } catch (IllegalAccessException e) {
            Log.e("PropertyValuesHolder", e.toString());
        }
    }

    void setupStartValue(Object target) {
        setupValue(target, mKeyframeSet.mKeyframes.get(0));
    }

    /**
     * This function is called by ObjectAnimator when setting the end values for an animation.
     * The end values are set according to the current values in the target object. The
     * property whose value is extracted is whatever is specified by the propertyName of this
     * PropertyValuesHolder object.
     *
     * @param target The object which holds the start values that should be set.
     */
    void setupEndValue(Object target) {
        setupValue(target, mKeyframeSet.mKeyframes.get(mKeyframeSet.mKeyframes.size() - 1));
    }

    @Override
    public PropertyValuesHolder clone() {
        try {
            PropertyValuesHolder newPVH = (PropertyValuesHolder) super.clone();
            newPVH.mPropertyName = mPropertyName;
            newPVH.mProperty = mProperty;
            newPVH.mKeyframeSet = mKeyframeSet.clone();
            newPVH.mEvaluator = mEvaluator;
            return newPVH;
        } catch (CloneNotSupportedException e) {
            // won't reach here
            return null;
        }
    }

    void setAnimatedValue(Object target) {
        if (mProperty != null) {
            mProperty.set(target, getAnimatedValue());
        }
        if (mSetter != null) {
            try {
                mTmpValueArray[0] = getAnimatedValue();
                mSetter.invoke(target, mTmpValueArray);
            } catch (InvocationTargetException e) {
                Log.e("PropertyValuesHolder", e.toString());
            } catch (IllegalAccessException e) {
                Log.e("PropertyValuesHolder", e.toString());
            }
        }
    }

    /**
     * Internal function, called by ValueAnimator, to set up the TypeEvaluator that will be used
     * to calculate animated values.
     */
    void init() {
        if (mEvaluator == null) {
            // We already handle int and float automatically, but not their Object
            // equivalents
            mEvaluator = (mValueType == Integer.class) ? sIntEvaluator :
                    (mValueType == Float.class) ? sFloatEvaluator :
                    null;
        }
        if (mEvaluator != null) {
            // KeyframeSet knows how to evaluate the common types - only give it a custom
            // evaluator if one has been set on this class
            mKeyframeSet.setEvaluator(mEvaluator);
        }
    }

    public void setEvaluator(TypeEvaluator evaluator) {
        mEvaluator = evaluator;
        mKeyframeSet.setEvaluator(evaluator);
    }

    void calculateValue(float fraction) {
        mAnimatedValue = mKeyframeSet.getValue(fraction);
    }

    public void setPropertyName(String propertyName) {
        mPropertyName = propertyName;
    }

    public void setProperty(Property property) {
        mProperty = property;
    }

    public String getPropertyName() {
        return mPropertyName;
    }

    /**
     * Internal function, called by ValueAnimator and ObjectAnimator, to retrieve the value
     * most recently calculated in calculateValue().
     * @return
     */
    Object getAnimatedValue() {
        return mAnimatedValue;
    }

    @Override
    public String toString() {
        return mPropertyName + ": " + mKeyframeSet.toString();
    }

    static String getMethodName(String prefix, String propertyName) {
        if (propertyName == null || propertyName.length() == 0) {
            // shouldn't get here
            return prefix;
        }
        char firstLetter = Character.toUpperCase(propertyName.charAt(0));
        String theRest = propertyName.substring(1);
        return prefix + firstLetter + theRest;
    }

    static class IntPropertyValuesHolder extends PropertyValuesHolder {

        // Cache JNI functions to avoid looking them up twice
        //private static final HashMap<Class, HashMap<String, Integer>> sJNISetterPropertyMap =
        //        new HashMap<Class, HashMap<String, Integer>>();
        //int mJniSetter;
        private IntProperty mIntProperty;

        IntKeyframeSet mIntKeyframeSet;
        int mIntAnimatedValue;

        public IntPropertyValuesHolder(String propertyName, IntKeyframeSet keyframeSet) {
            super(propertyName);
            mValueType = int.class;
            mKeyframeSet = keyframeSet;
            mIntKeyframeSet = (IntKeyframeSet) mKeyframeSet;
        }

        public IntPropertyValuesHolder(Property property, IntKeyframeSet keyframeSet) {
            super(property);
            mValueType = int.class;
            mKeyframeSet = keyframeSet;
            mIntKeyframeSet = (IntKeyframeSet) mKeyframeSet;
            if (property instanceof  IntProperty) {
                mIntProperty = (IntProperty) mProperty;
            }
        }

        public IntPropertyValuesHolder(String propertyName, int... values) {
            super(propertyName);
            setIntValues(values);
        }

        public IntPropertyValuesHolder(Property property, int... values) {
            super(property);
            setIntValues(values);
            if (property instanceof  IntProperty) {
                mIntProperty = (IntProperty) mProperty;
            }
        }

        @Override
        public void setIntValues(int... values) {
            super.setIntValues(values);
            mIntKeyframeSet = (IntKeyframeSet) mKeyframeSet;
        }

        @Override
        void calculateValue(float fraction) {
            mIntAnimatedValue = mIntKeyframeSet.getIntValue(fraction);
        }

        @Override
        Object getAnimatedValue() {
            return mIntAnimatedValue;
        }

        @Override
        public IntPropertyValuesHolder clone() {
            IntPropertyValuesHolder newPVH = (IntPropertyValuesHolder) super.clone();
            newPVH.mIntKeyframeSet = (IntKeyframeSet) newPVH.mKeyframeSet;
            return newPVH;
        }

        @Override
        void setAnimatedValue(Object target) {
            if (mIntProperty != null) {
                mIntProperty.setValue(target, mIntAnimatedValue);
                return;
            }
            if (mProperty != null) {
                mProperty.set(target, mIntAnimatedValue);
                return;
            }
            //if (mJniSetter != 0) {
            //    nCallIntMethod(target, mJniSetter, mIntAnimatedValue);
            //    return;
            //}
            if (mSetter != null) {
                try {
                    mTmpValueArray[0] = mIntAnimatedValue;
                    mSetter.invoke(target, mTmpValueArray);
                } catch (InvocationTargetException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                } catch (IllegalAccessException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                }
            }
        }

        @Override
        void setupSetter(Class targetClass) {
            if (mProperty != null) {
                return;
            }
                super.setupSetter(targetClass);
            //}
        }
    }

    static class FloatPropertyValuesHolder extends PropertyValuesHolder {

        // Cache JNI functions to avoid looking them up twice
        //private static final HashMap<Class, HashMap<String, Integer>> sJNISetterPropertyMap =
        //        new HashMap<Class, HashMap<String, Integer>>();
        //int mJniSetter;
        private FloatProperty mFloatProperty;

        FloatKeyframeSet mFloatKeyframeSet;
        float mFloatAnimatedValue;

        public FloatPropertyValuesHolder(String propertyName, FloatKeyframeSet keyframeSet) {
            super(propertyName);
            mValueType = float.class;
            mKeyframeSet = keyframeSet;
            mFloatKeyframeSet = (FloatKeyframeSet) mKeyframeSet;
        }

        public FloatPropertyValuesHolder(Property property, FloatKeyframeSet keyframeSet) {
            super(property);
            mValueType = float.class;
            mKeyframeSet = keyframeSet;
            mFloatKeyframeSet = (FloatKeyframeSet) mKeyframeSet;
            if (property instanceof FloatProperty) {
                mFloatProperty = (FloatProperty) mProperty;
            }
        }

        public FloatPropertyValuesHolder(String propertyName, float... values) {
            super(propertyName);
            setFloatValues(values);
        }

        public FloatPropertyValuesHolder(Property property, float... values) {
            super(property);
            setFloatValues(values);
            if (property instanceof  FloatProperty) {
                mFloatProperty = (FloatProperty) mProperty;
            }
        }

        @Override
        public void setFloatValues(float... values) {
            super.setFloatValues(values);
            mFloatKeyframeSet = (FloatKeyframeSet) mKeyframeSet;
        }

        @Override
        void calculateValue(float fraction) {
            mFloatAnimatedValue = mFloatKeyframeSet.getFloatValue(fraction);
        }

        @Override
        Object getAnimatedValue() {
            return mFloatAnimatedValue;
        }

        @Override
        public FloatPropertyValuesHolder clone() {
            FloatPropertyValuesHolder newPVH = (FloatPropertyValuesHolder) super.clone();
            newPVH.mFloatKeyframeSet = (FloatKeyframeSet) newPVH.mKeyframeSet;
            return newPVH;
        }

        /**
         * Internal function to set the value on the target object, using the setter set up
         * earlier on this PropertyValuesHolder object. This function is called by ObjectAnimator
         * to handle turning the value calculated by ValueAnimator into a value set on the object
         * according to the name of the property.
         * @param target The target object on which the value is set
         */
        @Override
        void setAnimatedValue(Object target) {
            if (mFloatProperty != null) {
                mFloatProperty.setValue(target, mFloatAnimatedValue);
                return;
            }
            if (mProperty != null) {
                mProperty.set(target, mFloatAnimatedValue);
                return;
            }
            //if (mJniSetter != 0) {
            //    nCallFloatMethod(target, mJniSetter, mFloatAnimatedValue);
            //    return;
            //}
            if (mSetter != null) {
                try {
                    mTmpValueArray[0] = mFloatAnimatedValue;
                    mSetter.invoke(target, mTmpValueArray);
                } catch (InvocationTargetException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                } catch (IllegalAccessException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                }
            }
        }

        @Override
        void setupSetter(Class targetClass) {
            if (mProperty != null) {
                return;
            }
                super.setupSetter(targetClass);
            //}
        }

    }
}
