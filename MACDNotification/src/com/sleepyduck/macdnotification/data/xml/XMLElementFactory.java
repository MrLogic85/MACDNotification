package com.sleepyduck.macdnotification.data.xml;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Utility class to transform a {@link IXMLParsable} to a XML string
 * 
 * @author Fredrik Metcalf
 */
public class XMLElementFactory implements Serializable {
	private static final long serialVersionUID = -4105768870585070475L;

	/**
	 * Builds a XMLElement structure from a XML String using a {@link SAXParser}
	 * 
	 * @param xmlString
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<XMLElement> BuildFromXMLString(final String xmlString) throws ParserConfigurationException, SAXException, IOException {
		final XMLElement startElement = new XMLElement();
		final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		final StringReader reader = new StringReader(xmlString);
		parser.parse(new InputSource(reader), new DefaultHandler() {
			XMLElement currentElement = startElement;

			@Override
			public void characters(final char[] ch, final int start, final int length) throws SAXException {
				int offset = 0;
				for (int i = 0; i < length; ++i)
					if (Character.isWhitespace(ch[start + i]))
						offset++;
					else
						break;

				currentElement.setData(String.valueOf(ch, start + offset, length - offset));
			}

			@Override
			public void endElement(final String uri, final String localName, final String qName) throws SAXException {
				currentElement = currentElement.getParent();
			}

			@Override
			public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
				final XMLElement newEl = new XMLElement(qName);
				currentElement.addChild(newEl);
				for (int i = 0; i < attributes.getLength(); ++i)
					newEl.addAttribute(attributes.getLocalName(i), attributes.getValue(i));
				currentElement = newEl;
			}
		});
		return startElement.getChildren();
	}

	private final IXMLParsable mRegisteredXMLObject;

	public XMLElementFactory(final IXMLParsable registeredXMLObject) {
		super();
		mRegisteredXMLObject = registeredXMLObject;
	}

	/**
	 * Creates a {@link XMLElement} representation of the {@link IXMLParsable} object
	 * 
	 * @return
	 */
	public XMLElement toXMLElement() {
		final XMLElement element = new XMLElement(mRegisteredXMLObject.getClass().getSimpleName());
		mRegisteredXMLObject.putAttributes(element);
		return element;
	}

	/**
	 * @return the XML String representation from {@link XMLElement#toXMLString()}
	 */
	public String toXMLString() {
		return toXMLElement().toString();
	}

}