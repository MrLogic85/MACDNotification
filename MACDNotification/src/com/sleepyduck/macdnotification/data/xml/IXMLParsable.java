package com.sleepyduck.macdnotification.data.xml;


/**
 * This interface is implemented by all classes that can be transformed to a {@link XMLElement} structure by {@link XMLElementFactory}
 * 
 * @author Fredrik Metcalf
 */
public interface IXMLParsable {

	/**
	 * Put the attributes of the IXMLParsable into the element. XMLElement children can also be
     * added to the element.
	 */
	public abstract void putAttributes(final XMLElement element);

	/**
	 * Creates a XMLElement representation of this message
	 */
	public abstract XMLElement toXMLElement();

}