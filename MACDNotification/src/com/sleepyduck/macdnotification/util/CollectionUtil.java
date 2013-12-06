package com.sleepyduck.macdnotification.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class CollectionUtil {
	public static <F, E extends Collection<F>> E filter(E collection, Filter<F> filter) {
		try {
			Constructor<? extends Collection> constructor = collection.getClass().getConstructor();
			E newCollection = (E) constructor.newInstance();
			for (F item : collection)
				if (filter.filter(item))
					newCollection.add(item);
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

	public interface Filter<F> {
		public boolean filter(F object);
	}
}
