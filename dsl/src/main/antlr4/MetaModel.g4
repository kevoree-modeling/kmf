grammar MetaModel;

fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

STRING :  '"' (ESC | ~["\\])* '"' | '\'' (ESC | ~["\\])* '\'' ;
IDENT : [a-zA-Z_][a-zA-Z_0-9]*;
TYPE_NAME : [a-zA-Z_][.a-zA-Z_0-9]*;
NUMBER : [\-]?[0-9]+'.'?[0-9]*;
WS : ([ \t\r\n]+ | SL_COMMENT) -> skip ; // skip spaces, tabs, newlines
SL_COMMENT :  '//' ~('\r' | '\n')* ;

metamodel: (enumDeclr | classDeclr | indexDeclr)*;

indexDeclr : 'index' IDENT ':' (TYPE_NAME|IDENT) '{' indexLiterals '}';
indexLiterals : IDENT (',' IDENT)*;

enumDeclr : 'enum' (TYPE_NAME|IDENT) '{' enumLiterals '}';
enumLiterals : IDENT (',' IDENT)*;
classDeclr : 'class' (TYPE_NAME|IDENT) parentsDeclr? '{' (attributeDeclaration | relationDeclaration)* '}';
parentsDeclr : 'extends' (TYPE_NAME|IDENT);
semanticDeclr : '{' (semanticUsing | semanticFrom | semanticWith )* '}' ;
semanticWith : 'with' IDENT (STRING|NUMBER);
semanticUsing : 'using' STRING;
semanticFrom : 'from' STRING;

annotation : ('learned' | 'derived' | 'global');

attributeType : 'String' | 'Double' | 'Long' | 'Integer' | 'Boolean' | TYPE_NAME;
attributeDeclaration : annotation* 'att' IDENT ':' attributeType semanticDeclr? ;

relationDeclaration : annotation* 'rel' IDENT ':' (TYPE_NAME|IDENT) semanticDeclr? ;
