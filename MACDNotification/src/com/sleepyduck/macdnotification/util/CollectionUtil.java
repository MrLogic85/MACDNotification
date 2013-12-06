package com.sleepyduck.macdnotification.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class CollectionUtil {
	public static <F, E extends Collection<F>> E filter(E collection, Filter<F> filter) {
		try {
			@SuppressWarnings("unchecked")
			Constructor<E> constructor = (Constructor<E>) collection.getClass().getConstructor();
			E newCollection = constructor.newInstance();
			for (F item : collection) {
				if (filter.filter(item)) {
					newCollection.add(item);
				}
			}
			return newCollection;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return collection;
	}

	public static <E> int indexOf(List<E> list, Filter<E> filter) {
		for (int i = 0; i < list.size(); ++i) {
			if (filter.filter(list.get(i))) {
				return i;
			}
		}
		return -1;
	}

	public static <E> void foreach(Collection<E> collection, Executor<E> executor) {
		for (E e : collection) {
			executor.execute(e);
		}
	}

	public interface Filter<E> {
		public boolean filter(E object);
	}

	public interface Executor<E> {
		public void execute(E object);
	}
}
