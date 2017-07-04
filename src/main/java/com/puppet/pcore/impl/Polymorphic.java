package com.puppet.pcore.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public abstract class Polymorphic<T> {
	protected static class DispatchMap {
		private final String methodName;
		private final Map<Class<?>,Method> methods;

		private DispatchMap(String name, Map<Class<?>,Method> methods) {
			this.methodName = name;
			this.methods = methods;
		}

		private Method findMethod(Object receiver) {
			Method m;
			if(receiver == null) {
				m = methods.get(Void.class);
				if(m == null)
					throw new IllegalArgumentException(format("Don't know how to %s an undef", methodName));
			} else {
				Class<?> c = receiver.getClass();
				m = findMethod(c);
				if(m == null)
					throw new IllegalArgumentException(format("Don't know how to %s instance of class '%s'", methodName, c.getName()));
			}
			if(!m.isAccessible())
				m.setAccessible(true);
			return m;
		}

		private Method findMethod(Class<?> cls) {
			if(cls == null)
				return null;

			Method m = methods.get(cls);
			if(m == null) {
				for(Class<?> ifd : cls.getInterfaces()) {
					m = findMethod(ifd);
					if(m != null)
						break;
				}
				if(m == null)
					m = findMethod(cls.getSuperclass());
				if(m != null)
					methods.put(cls, m);
			}
			return m;
		}
	}

	protected static DispatchMap initPolymorphicDispatch(Class<?> receiverClass, String name) {
		return initPolymorphicDispatch(receiverClass, name, 0);
	}

	protected static DispatchMap initPolymorphicDispatch(Class<?> receiverClass, String name, int extraParams) {
		Map<Class<?>,Method> result = new ConcurrentHashMap<>();
		for(Method m : receiverClass.getDeclaredMethods()) {
			if(m.getName().equals(name) && (m.getModifiers() & Modifier.STATIC) == 0) {
				Class<?>[] paramTypes = m.getParameterTypes();
				if(paramTypes.length == 1 + extraParams)
					result.put(paramTypes[0], m);
			}
		}
		return new DispatchMap(name, result);
	}

	@SuppressWarnings("unchecked")
	protected T dispatch(Object ...args) {
		Method m = getDispatchMap().findMethod(args[0]);
		try {
			return (T)m.invoke(this, args);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			Throwable te = e.getCause();
			if(!(te instanceof RuntimeException))
				te = new RuntimeException(te);
			throw (RuntimeException)te;
		}
	}

	@SuppressWarnings("unchecked")
	protected T dispatchWOCatch(Object ...args) throws InvocationTargetException {
		Method m = getDispatchMap().findMethod(args[0]);
		try {
			return (T)m.invoke(this, args);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract DispatchMap getDispatchMap();
}
