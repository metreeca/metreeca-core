http://vrici.lojban.org/~cowan/XML/tagsoup/ current home
http://home.ccil.org/~cowan/XML/tagsoup/  original defunct home


# SAX Features

TagSoup supports the following SAX features in addition to the standard ones:

http://www.ccil.org/~cowan/tagsoup/features/ignore-bogons
A value of "true" indicates that the parser will ignore unknown elements.

http://www.ccil.org/~cowan/tagsoup/features/bogons-empty
A value of "true" indicates that the parser will give unknown elements a content model of EMPTY; a value of "false", a content model of ANY.

http://www.ccil.org/~cowan/tagsoup/features/root-bogons
A value of "true" indicates that the parser will allow unknown elements to be the root of the output document.

http://www.ccil.org/~cowan/tagsoup/features/default-attributes
A value of "true" indicates that the parser will return default attribute values for missing attributes that have default values.

http://www.ccil.org/~cowan/tagsoup/features/translate-colons
A value of "true" indicates that the parser will translate colons into underscores in names.

http://www.ccil.org/~cowan/tagsoup/features/restart-elements
A value of "true" indicates that the parser will attempt to restart the restartable elements.

http://www.ccil.org/~cowan/tagsoup/features/ignorable-whitespace
A value of "true" indicates that the parser will transmit whitespace in element-only content via the SAX ignorableWhitespace callback. Normally this is not done, because HTML is an SGML application and SGML suppresses such whitespace.

http://www.ccil.org/~cowan/tagsoup/features/cdata-elements
A value of "true" indicates that the parser will process the script and style elements (or any elements with type='cdata' in the TSSL schema) as SGML CDATA elements (that is, no markup is recognized except the matching end-tag).

# SAX Properties

http://www.ccil.org/~cowan/tagsoup/properties/scanner
Specifies the Scanner object this parser uses.

http://www.ccil.org/~cowan/tagsoup/properties/schema
Specifies the Schema object this parser uses.

http://www.ccil.org/~cowan/tagsoup/properties/auto-detector
Specifies the AutoDetector (for encoding detection) this parser uses.