package com.sleepyduck.macdnotification.data.xml;


/**
 * This interface is implemented by all classes that can be transformed to a {@link XMLElement} structure by {@link XMLElementFactory}
 * 
 * @author Fredrik Metcalf
 */
public interface IXMLParsable {

	/**
	 * Writes the attributes of the Message into a XMLStremWriter
	 * 
	 * @param out
	 *            the XMLStreamWriter to write the attributes to
	 */
	public abstract void putAttributes(final XMLElement element);

	/**
	 * Creates a XMLElement representation of this message
	 * 
	 * @return
	 */
	public abstract XMLElement toXMLElement();

}