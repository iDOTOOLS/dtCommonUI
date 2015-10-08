
package com.nineoldandroids.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import android.view.View;

import com.nineoldandroids.util.Property;
import com.nineoldandroids.view.animation.AnimatorProxy;

public final class ObjectAnimator extends ValueAnimator {
    private static final boolean DBG = false;
    private static final Map<String, Property> PROXY_PROPERTIES = new HashMap<String, Property>();

    static {
        PROXY_PROPERTIES.put("alpha", PreHoneycombCompat.ALPHA);
        PROXY_PROPERTIES.put("pivotX", PreHoneycombCompat.PIVOT_X);
        PROXY_PROPERTIES.put("pivotY", PreHoneycombCompat.PIVOT_Y);
        PROXY_PROPERTIES.put("translationX", PreHoneycombCompat.TRANSLATION_X);
        PROXY_PROPERTIES.put("translationY", PreHoneycombCompat.TRANSLATION_Y);
        PROXY_PROPERTIES.put("rotation", PreHoneycombCompat.ROTATION);
        PROXY_PROPERTIES.put("rotationX", PreHoneycombCompat.ROTATION_X);
        PROXY_PROPERTIES.put("rotationY", PreHoneycombCompat.ROTATION_Y);
        PROXY_PROPERTIES.put("scaleX", PreHoneycombCompat.SCALE_X);
        PROXY_PROPERTIES.put("scaleY", PreHoneycombCompat.SCALE_Y);
        PROXY_PROPERTIES.put("scrollX", PreHoneycombCompat.SCROLL_X);
        PROXY_PROPERTIES.put("scrollY", PreHoneycombCompat.SCROLL_Y);
        PROXY_PROPERTIES.put("x", PreHoneycombCompat.X);
        PROXY_PROPERTIES.put("y", PreHoneycombCompat.Y);
    }

    // The target object on which the property exists, set in the constructor
    private Object mTarget;

    private String mPropertyName;

    private Property mProperty;

    public void setPropertyName(String propertyName) {
        // mValues could be null if this is being constructed piecemeal. Just record the
        // propertyName to be used later when setValues() is called if so.
        if (mValues != null) {
            PropertyValuesHolder valuesHolder = mValues[0];
            String oldName = valuesHolder.getPropertyName();
            valuesHolder.setPropertyName(propertyName);
            mValuesMap.remove(oldName);
            mValuesMap.put(propertyName, valuesHolder);
        }
        mPropertyName = propertyName;
        // New property/values/target should cause re-initialization prior to starting
        mInitialized = false;
    }

    /**
     * Sets the property that will be animated. Property objects will take precedence over
     * properties specified by the {@link #setPropertyName(String)} method. Animations should
     * be set up to use one or the other, not both.
     *
     * @param property The property being animated. Should not be null.
     */
    public void setProperty(Property property) {
        // mValues could be null if this is being constructed piecemeal. Just record the
        // propertyName to be used later when setValues() is called if so.
        if (mValues != null) {
            PropertyValuesHolder valuesHolder = mValues[0];
            String oldName = valuesHolder.getPropertyName();
            valuesHolder.setProperty(property);
            mValuesMap.remove(oldName);
            mValuesMap.put(mPropertyName, valuesHolder);
        }
        if (mProperty != null) {
            mPropertyName = property.getName();
        }
        mProperty = property;
        // New property/values/target should cause re-initialization prior to starting
        mInitialized = false;
    }

    public String getPropertyName() {
        return mPropertyName;
    }

    public ObjectAnimator() {
    }
    private ObjectAnimator(Object target, String propertyName) {
        mTarget = target;
        setPropertyName(propertyName);
    }

    private <T> ObjectAnimator(T target, Property<T, ?> property) {
        mTarget = target;
        setProperty(property);
    }

    public static ObjectAnimator ofInt(Object target, String propertyName, int... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setIntValues(values);
        return anim;
    }

    public static <T> ObjectAnimator ofInt(T target, Property<T, Integer> property, int... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setIntValues(values);
        return anim;
    }

    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setFloatValues(values);
        return anim;
    }

    public static <T> ObjectAnimator ofFloat(T target, Property<T, Float> property,
            float... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setFloatValues(values);
        return anim;
    }

    public static ObjectAnimator ofObject(Object target, String propertyName,
            TypeEvaluator evaluator, Object... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setObjectValues(values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property,
            TypeEvaluator<V> evaluator, V... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setObjectValues(values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target,
            PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.mTarget = target;
        anim.setValues(values);
        return anim;
    }

    @Override
    public void setIntValues(int... values) {
        if (mValues == null || mValues.length == 0) {
            // No values yet - this animator is being constructed piecemeal. Init the values with
            // whatever the current propertyName is
            if (mProperty != null) {
                setValues(PropertyValuesHolder.ofInt(mProperty, values));
            } else {
                setValues(PropertyValuesHolder.ofInt(mPropertyName, values));
            }
        } else {
            super.setIntValues(values);
        }
    }

    @Override
    public void setFloatValues(float... values) {
        if (mValues == null || mValues.length == 0) {
            // No values yet - this animator is being constructed piecemeal. Init the values with
            // whatever the current propertyName is
            if (mProperty != null) {
                setValues(PropertyValuesHolder.ofFloat(mProperty, values));
            } else {
                setValues(PropertyValuesHolder.ofFloat(mPropertyName, values));
            }
        } else {
            super.setFloatValues(values);
        }
    }

    @Override
    public void setObjectValues(Object... values) {
        if (mValues == null || mValues.length == 0) {
            // No values yet - this animator is being constructed piecemeal. Init the values with
            // whatever the current propertyName is
            if (mProperty != null) {
                setValues(PropertyValuesHolder.ofObject(mProperty, (TypeEvaluator)null, values));
            } else {
                setValues(PropertyValuesHolder.ofObject(mPropertyName, (TypeEvaluator)null, values));
            }
        } else {
            super.setObjectValues(values);
        }
    }

    @Override
    public void start() {
        if (DBG) {
            Log.d("ObjectAnimator", "Anim target, duration: " + mTarget + ", " + getDuration());
            for (int i = 0; i < mValues.length; ++i) {
                PropertyValuesHolder pvh = mValues[i];
                ArrayList<Keyframe> keyframes = pvh.mKeyframeSet.mKeyframes;
                Log.d("ObjectAnimator", "   Values[" + i + "]: " +
                    pvh.getPropertyName() + ", " + keyframes.get(0).getValue() + ", " +
                    keyframes.get(pvh.mKeyframeSet.mNumKeyframes - 1).getValue());
            }
        }
        super.start();
    }

    @Override
    void initAnimation() {
        if (!mInitialized) {
            // mValueType may change due to setter/getter setup; do this before calling super.init(),
            // which uses mValueType to set up the default type evaluator.
            if ((mProperty == null) && AnimatorProxy.NEEDS_PROXY && (mTarget instanceof View) && PROXY_PROPERTIES.containsKey(mPropertyName)) {
                setProperty(PROXY_PROPERTIES.get(mPropertyName));
            }
            int numValues = mValues.length;
            for (int i = 0; i < numValues; ++i) {
                mValues[i].setupSetterAndGetter(mTarget);
            }
            super.initAnimation();
        }
    }

    @Override
    public ObjectAnimator setDuration(long duration) {
        super.setDuration(duration);
        return this;
    }


    /**
     * The target object whose property will be animated by this animation
     *
     * @return The object being animated
     */
    public Object getTarget() {
        return mTarget;
    }

    /**
     * Sets the target object whose property will be animated by this animation
     *
     * @param target The object being animated
     */
    @Override
    public void setTarget(Object target) {
        if (mTarget != target) {
            final Object oldTarget = mTarget;
            mTarget = target;
            if (oldTarget != null && target != null && oldTarget.getClass() == target.getClass()) {
                return;
            }
            // New target type should cause re-initialization prior to starting
            mInitialized = false;
        }
    }

    @Override
    public void setupStartValues() {
        initAnimation();
        int numValues = mValues.length;
        for (int i = 0; i < numValues; ++i) {
            mValues[i].setupStartValue(mTarget);
        }
    }

    @Override
    public void setupEndValues() {
        initAnimation();
        int numValues = mValues.length;
        for (int i = 0; i < numValues; ++i) {
            mValues[i].setupEndValue(mTarget);
        }
    }

    @Override
    void animateValue(float fraction) {
        super.animateValue(fraction);
        int numValues = mValues.length;
        for (int i = 0; i < numValues; ++i) {
            mValues[i].setAnimatedValue(mTarget);
        }
    }

    @Override
    public ObjectAnimator clone() {
        final ObjectAnimator anim = (ObjectAnimator) super.clone();
        return anim;
    }

    @Override
    public String toString() {
        String returnVal = "ObjectAnimator@" + Integer.toHexString(hashCode()) + ", target " +
            mTarget;
        if (mValues != null) {
            for (int i = 0; i < mValues.length; ++i) {
                returnVal += "\n    " + mValues[i].toString();
            }
        }
        return returnVal;
    }
}
