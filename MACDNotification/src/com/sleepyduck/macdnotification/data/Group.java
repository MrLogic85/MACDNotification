package com.sleepyduck.macdnotification.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.sleepyduck.macdnotification.data.xml.XMLElement;
import com.sleepyduck.macdnotification.data.xml.XMLParsableAdaptor;

public class Group extends XMLParsableAdaptor implements Parcelable {
	private static final long serialVersionUID = 614420628326821169L;

	public static final Parcelable.Creator<Group> CREATOR = new Parcelable.Creator<Group>() {
		@Override
		public Group createFromParcel(Parcel in) {
			return new Group(in);
		}

		@Override
		public Group[] newArray(int size) {
			return new Group[size];
		}
	};

	private String mName = "";
	private List<Symbol> mSymbols = new ArrayList<Symbol>();

	public Group(Parcel in) {
		mName = in.readString();
		Collections.addAll(mSymbols, (Symbol[]) in.readParcelableArray(Symbol.class.getClassLoader()));
	}

	public Group(String name) {
		mName = name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeParcelableArray(mSymbols.toArray(new Symbol[mSymbols.size()]), flags);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof String) {
			return mName.equals(other);
		}
		return super.equals(other);
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
		for (int i = 0; i < mSymbols.size(); ++i) {
			symbols.addChild(mSymbols.get(i).toXMLElement());
		}
	}

	public String getName() {
		return mName;
	}

	public Symbol getSymbol(String symbolName) {
		for (Symbol symbol : mSymbols) {
			if (symbol.getName().equals(symbolName)) {
				return symbol;
			}
		}
		return null;
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
