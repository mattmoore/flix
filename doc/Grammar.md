# Flix Grammar #

```
Program            =  D ...                         Declaration
            
Declaration     D  =  namespace Name                Namespace
                   |                                Function
                   |                                 Law
                   |                                Signature
                                                    Enum
                                                    Relation
                                                    Lattice
                                                    Index
                   |  c                             Constraint


Value           v  =  ()                            Unit
                   |  true | false                  Boolean
                   |  [Int]                         Int
                   |  [Str]                         Str
                   |  Name.v                        Tagged
                   |  (v_1, ..., v_n)               Tuple
               

Expr            e  =  v                             Value
                   |  x                             Var
                   |  e(e_1, ..., e_n)              App
                   |  e_1 `e` e_2                   Infix
                   |  op e                          Unary
                   |  e_1 op e_2                    Binary
                   |  let x = e in e                Let-binding
                   |  if (e) then e else e          If-then-else
                   |  switch { SCase ... }          Switch
                   |  match e with { MCase... }     Match
                   |  Tag
                   | Tuple
                   | Ascribe
                   | Error
                   | bot | top
               
               
Pattern         p  =  _                             Wildcard
                   |  x                             Variable
                   |  true | false                  Boolean
                   |  ...  
                   |  Tag
                   |  tupl

rule

predicate

term
wildcard, var, lit, appyl, infix, ascribe

Type        T  =  Name                          Named type
               |  Unit                          Unit type
               |  Bool                          Bool type
               |  Char                          Char type
               |  Int                           Alias for Int32
               |  Int8                          Unsigned  8 bit int type
               |  Int16                         Unsigned 16 bit int type
               |  Int32                         Unsigned 32 bit int type
               |  Int64                         Unsigned 64 bit int type
               |  Str                           String type
               |  Native                        Native type
               |  (T_1, ..., T_n)               Tuple type
               |  (T1, ..., T_n) -> T           Lambda type
               |  Opt[T]                        Option type
               |  Lst[T]                        List type
               |  Set[T]                        Set type
               |  Map[T_1, T_2]                 Map type

```