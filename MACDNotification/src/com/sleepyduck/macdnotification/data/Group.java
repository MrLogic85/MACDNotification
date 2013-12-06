package com.sleepyduck.macdnotification.data;

import java.util.ArrayList;
import java.util.List;

import com.sleepyduck.macdnotification.data.xml.XMLElement;
import com.sleepyduck.macdnotification.data.xml.XMLParsableAdaptor;

public class Group extends XMLParsableAdaptor {
	private static final long serialVersionUID = 614420628326821169L;

	private String mName = "";
	private List<Symbol> mSymbols = new ArrayList<Symbol>();

	public Group(String name) {
		mName = name;
	}

	public Group(XMLElement element) {
		mName = element.getAttribute("name", "");
		XMLElement symbols = element.getElement("Symbols");
		if (symbols != null) {
			for (XMLElement xmlSymbol : symbols.getChildren()) {
				Symbol symbol = new Symbol(xmlSymbol);
				mSymbols.add(symbol);
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Group) {
			return mName.equals(((Group) other).getName());
		}
		return super.equals(other);
	}

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
	public String toString() {
		return mName;
	}

	@Override
	public void putAttributes(XMLElement element) {
		super.putAttributes(element);
		element.addAttribute("name", mName);
		XMLElement symbols = element.addChild(new XMLElement("Symbols"));
        for (Symbol mSymbol : mSymbols) {
            symbols.addChild(mSymbol.toXMLElement());
        }
	}

	public String getName() {
		return mName;
	}

    public Symbol getSymbol(int i) {
		if (mSymbols.size() > i)
			return mSymbols.get(i);
		return null;
	}

	public Symbol addSymbol(String name) {
		Symbol symbol = new Symbol(name);
		mSymbols.add(symbol);
		return symbol;
	}

	public List<Symbol> getSymbols() {
		return mSymbols;
	}

	public Symbol removeSymbol(int i) {
		if (mSymbols.size() > i)
			return mSymbols.remove(i);
		return null;
	}

}
