package com.sleepyduck.macdnotification.data.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a XML Element and can be used to build a xml document
 * 
 * @author Fredrik Metcalf
 */
public class XMLElement implements Serializable {
	/**
	 * A name vaule pair used to store attributes in the {@link XMLElement}
	 * 
	 * @author Fredrik Metcalf
	 */
	public class Attribute implements Serializable {
		private static final long serialVersionUID = -2539266391216584081L;

		final String Name;
		final String Value;

		public Attribute(final String name, final String value) {
			Name = name;
			Value = value;
		}
	}

	private static final long serialVersionUID = 8743012699148733484L;

	private String mName = "";
	private String mData = "";
	private final List<XMLElement> mChildren = new ArrayList<XMLElement>();

	private XMLElement mParent = null;
	private final List<Attribute> mAttributes = new ArrayList<Attribute>();

	public XMLElement() {

	}

	public XMLElement(final String name) {
		this();
		this.mName = name;
	}

	public XMLElement(final String name, final String data) {
		this(name);
		this.mData = data;
	}

	public XMLElement(final String name, final String data, final XMLElement... children) {
		this(name, data);
		mChildren.addAll(Arrays.asList(children));
	}

	/**
	 * Internal {@link #toString()} used to create the XML String output.
	 * 
	 * @param ident
	 * @return
	 */
	private String toString(final String ident) {
		String str = ident + "<" + mName + "";
		if (mAttributes.size() > 0) {
			if (mAttributes.size() == 1) {
				str += " " + getAttributeName(0) + "=\"" + getAttributeValue(0) + "\"";
			} else for (int i = 0; i < mAttributes.size(); ++i) {
				str += "\n" + ident + "\t" + getAttributeName(i) + "=\"" + getAttributeValue(i) + "\"";
			}
		}
		if (mData.length() > 0 || mChildren.size() > 0)
			str += ">" + mData + (mChildren.size() > 0 ? "\n" : "");
		else
			str += "/>";
		for (final XMLElement el : mChildren)
			str += el.toString(ident + "\t") + "\n";
		if (mChildren.size() > 0)
			str += (mChildren.size() > 0 ? ident : "") + "</" + mName + ">";
		return str;
	}

	/**
	 * Adds all the {@link XMLElement XMLElements} in the list as children of this XMLElement
	 * 
	 * @param elems
	 */
	public void addAllChildren(final List<XMLElement> elems) {
		mChildren.addAll(elems);
		for (final XMLElement el : elems)
			el.mParent = this;
	}

	/**
	 * Adds a {@link Attribute}
	 * 
	 * @param name
	 * @param value
	 */
	public void addAttribute(final String name, final String value) {
		mAttributes.add(new Attribute(name, value));
	}

	/**
	 * Adds the {@link XMLElement} to the the end of the child list
	 * 
	 * @param el
	 */
	public XMLElement addChild(final XMLElement el) {
		mChildren.add(el);
		el.mParent = this;
		return el;
	}

	/**
	 * Returns the {@link Attribute} at index i
	 * 
	 * @param i
	 * @return
	 */
	public Attribute getAttribute(final int i) {
		return mAttributes.get(i);
	}

	/**
	 * Returns the {@link Attribute#Value} of the {@link Attribute} with the supplied {@link Attribute#Name}
	 * 
	 * @param name
	 * @return
	 */
	public String getAttribute(final String name) {
		for (final Attribute a : mAttributes)
			if (a.Name.equals(name))
				return a.Value;
		return null;
	}

	public String getAttribute(String name, String defaultName) {
		for (final Attribute a : mAttributes)
			if (a.Name.equals(name))
				return a.Value;
		return defaultName;
	}

	/**
	 * Return the {@link Attribute#Name} at index i
	 * 
	 * @param i
	 * @return
	 */
	public String getAttributeName(final int i) {
		return mAttributes.get(i).Name;
	}

	/**
	 * Returns the {@link Attribute#Value} at index i
	 * 
	 * @param i
	 * @return
	 */
	public String getAttributeValue(final int i) {
		return mAttributes.get(i).Value;
	}

	/**
	 * Returns all children
	 * 
	 * @return
	 */
	public List<XMLElement> getChildren() {
		return mChildren;
	}

	/**
	 * Returns the data
	 * 
	 * @return
	 */
	public String getData() {
		return mData;
	}

	/**
	 * Returns the first occurrence of a {@link XMLElement} with the given name, searches depth first.
	 * 
	 * @param name
	 * @return
	 */
	public XMLElement getElement(final String name) {
		if (mName.equals(name))
			return this;
		for (final XMLElement el : mChildren) {
			final XMLElement foundEl = el.getElement(name);
			if (foundEl != null)
				return foundEl;
		}
		return null;
	}

	/**
	 * Returns the first occurrence of a {@link XMLElement} with the given element name and attribute name-value pair, searches depth first.
	 * 
	 * @param elementName
	 * @param attributeName
	 * @param attributeValue
	 * @return
	 */
	public XMLElement getElement(final String elementName, final String attributeName, final String attributeValue) {
		if (mName.equals(elementName) && getAttribute(attributeName) != null
				&& getAttribute(attributeName).equals(attributeValue)) {
			return this;
		}
		for (final XMLElement el : mChildren) {
			final XMLElement foundEl = el.getElement(elementName, attributeName, attributeValue);
			if (foundEl != null)
				return foundEl;
		}
		return null;
	}

	/**
	 * Returns the name of this element
	 * 
	 * @return
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Returns the parent {@link XMLElement}
	 * 
	 * @return
	 */
	public XMLElement getParent() {
		return mParent;
	}

	/**
	 * Sets the data of this element
	 * 
	 * @param data
	 */
	public void setData(final String data) {
		this.mData = data;
	}

	/**
	 * Returns a XML String
	 * 
	 * @param ident
	 * @return
	 */
	@Override
	public String toString() {
		return toString("");
	}
}
