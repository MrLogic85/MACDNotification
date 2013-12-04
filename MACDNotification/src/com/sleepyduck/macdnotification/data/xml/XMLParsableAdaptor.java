package com.sleepyduck.macdnotification.data.xml;

import java.io.Serializable;


/**
 * Adaptor to the {@link IXMLParsable} Interface
 * 
 * @author Fredrik Metcalf
 */
public class XMLParsableAdaptor implements IXMLParsable, Serializable {
	private static final long serialVersionUID = 3650027401908677306L;

	private final XMLElementFactory mXMLElementFactory;

	public XMLParsableAdaptor() {
		mXMLElementFactory = new XMLElementFactory(this);
	}

	@Override
	public void putAttributes(final XMLElement element) {}

	@Override
	public XMLElement toXMLElement() {
		return mXMLElementFactory.toXMLElement();
	}

	/**
	 * @return the XML {@link String} representation from {@link XMLElementFactory}
	 */
	public String toXMLString() {
		return mXMLElementFactory.toXMLString();
	}

}
