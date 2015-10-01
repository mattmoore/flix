package ca.uwaterloo.flix.language.phase

import ca.uwaterloo.flix.language.Compiler
import ca.uwaterloo.flix.language.ast._

import org.scalatest.FunSuite

class TestTyper extends FunSuite {

  // TODO: Consider using real syntax?

  val SL = SourceLocation.Unknown
  val Root = ResolvedAst.Root(Map.empty, Map.empty, Map.empty, Map.empty, List.empty, List.empty)
  val Ident = Name.Ident("x", SourceLocation.Unknown)
  val RName = Name.Resolved(List("foo", "bar"))

  /////////////////////////////////////////////////////////////////////////////
  // Definitions                                                             //
  /////////////////////////////////////////////////////////////////////////////

  test("Definition.Constant01") {
    val exp = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL)
    val tpe = ResolvedAst.Type.Unit
    val rast = ResolvedAst.Definition.Constant(RName, exp, tpe, SL)

    val result = Typer.Definition.typer(rast, Root)
    assertResult(TypedAst.Type.Unit)(result.get.tpe)
  }

  test("Definition.Constant02") {
    val exp = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val tpe = ResolvedAst.Type.Bool
    val rast = ResolvedAst.Definition.Constant(RName, exp, tpe, SL)

    val result = Typer.Definition.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  test("Definition.Constant03") {
    val exp = ResolvedAst.Expression.Binary(
      BinaryOperator.Plus,
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(21, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL), SL)
    val tpe = ResolvedAst.Type.Int
    val rast = ResolvedAst.Definition.Constant(RName, exp, tpe, SL)

    val result = Typer.Definition.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Definition.Constant04") {
    val x = Name.Ident("x", SourceLocation.Unknown)

    // fn (x: Unit): Unit = ()
    val exp = ResolvedAst.Expression.Lambda(
      formals = List(ResolvedAst.FormalArg(x, ResolvedAst.Type.Unit)),
      retTpe = ResolvedAst.Type.Unit,
      body = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL), SL)
    val tpe = ResolvedAst.Type.Function(List(ResolvedAst.Type.Unit), ResolvedAst.Type.Unit)
    val rast = ResolvedAst.Definition.Constant(RName, exp, tpe, SL)

    val result = Typer.Definition.typer(rast, Root)
    val expectedType = TypedAst.Type.Lambda(List(TypedAst.Type.Unit), TypedAst.Type.Unit)
    assertResult(expectedType)(result.get.tpe)
  }

  test("Definition.Constant05") {
    val x = Name.Ident("x", SourceLocation.Unknown)

    // fn (x: Int): Int = x
    val exp = ResolvedAst.Expression.Lambda(
      formals = List(ResolvedAst.FormalArg(x, ResolvedAst.Type.Int)),
      retTpe = ResolvedAst.Type.Int,
      body = ResolvedAst.Expression.Var(x, SL), SL)
    val tpe = ResolvedAst.Type.Function(List(ResolvedAst.Type.Int), ResolvedAst.Type.Int)
    val rast = ResolvedAst.Definition.Constant(RName, exp, tpe, SL)

    val result = Typer.Definition.typer(rast, Root)
    val expectedType = TypedAst.Type.Lambda(List(TypedAst.Type.Int), TypedAst.Type.Int)
    assertResult(expectedType)(result.get.tpe)
  }

  test("Definition.Lattice01") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)

    val rast = ResolvedAst.Definition.Lattice(
      elms = ResolvedAst.Type.Bool,
      bot = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(false, SL), SL),
      leq = ResolvedAst.Expression.Lambda(
        formals = List(
          ResolvedAst.FormalArg(x, ResolvedAst.Type.Bool),
          ResolvedAst.FormalArg(y, ResolvedAst.Type.Bool)),
        retTpe = ResolvedAst.Type.Bool,
        body = ResolvedAst.Expression.Binary(
          BinaryOperator.Or,
          ResolvedAst.Expression.Unary(UnaryOperator.Not, ResolvedAst.Expression.Var(x, SL), SL),
          ResolvedAst.Expression.Var(y, SL), SL), SL
      ),
      lub = ResolvedAst.Expression.Lambda(
        formals = List(
          ResolvedAst.FormalArg(x, ResolvedAst.Type.Bool),
          ResolvedAst.FormalArg(y, ResolvedAst.Type.Bool)),
        retTpe = ResolvedAst.Type.Bool,
        body = ResolvedAst.Expression.Binary(
          BinaryOperator.Or,
          ResolvedAst.Expression.Var(x, SL),
          ResolvedAst.Expression.Var(y, SL), SL), SL
      ), SL)

    val result = Typer.Definition.typer(rast, Root)
    assert(result.isSuccess)
  }

  test("Definition.Lattice.TypeError") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)

    val rast = ResolvedAst.Definition.Lattice(
      elms = ResolvedAst.Type.Str,
      bot = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(false, SL), SL),
      leq = ResolvedAst.Expression.Lambda(
        formals = List(
          ResolvedAst.FormalArg(x, ResolvedAst.Type.Int),
          ResolvedAst.FormalArg(y, ResolvedAst.Type.Str)),
        retTpe = ResolvedAst.Type.Int,
        body = ResolvedAst.Expression.Var(x, SL), SL),
      lub = ResolvedAst.Expression.Lambda(
        formals = List(
          ResolvedAst.FormalArg(x, ResolvedAst.Type.Bool),
          ResolvedAst.FormalArg(y, ResolvedAst.Type.Bool)),
        retTpe = ResolvedAst.Type.Int,
        body = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
        , SL), SL)

    val result = Typer.Definition.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Definition.Relation01") {
    val rast = ResolvedAst.Definition.Relation(RName, List(
      ResolvedAst.Attribute(Ident, ResolvedAst.Type.Bool)
    ), SL)

    val result = Typer.Definition.typer(rast, Root)
    assert(result.isSuccess)
  }

  test("Definition.Relation02") {
    val rast = ResolvedAst.Definition.Relation(RName, List(
      ResolvedAst.Attribute(Ident, ResolvedAst.Type.Bool),
      ResolvedAst.Attribute(Ident, ResolvedAst.Type.Int),
      ResolvedAst.Attribute(Ident, ResolvedAst.Type.Str)
    ), SL)

    val result = Typer.Definition.typer(rast, Root)
    assert(result.isSuccess)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Constraints                                                             //
  /////////////////////////////////////////////////////////////////////////////
  test("Constraint.Fact01") {
    val rname = Name.Resolved(List("Student"))

    val root = Root.copy(relations = Map(
      rname -> ResolvedAst.Definition.Relation(rname, List(
        ResolvedAst.Attribute(Ident, ResolvedAst.Type.Str),
        ResolvedAst.Attribute(Ident, ResolvedAst.Type.Int)
      ), SL)
    ))

    val rast = ResolvedAst.Constraint.Fact(ResolvedAst.Predicate.Head(
      rname, List(
        ResolvedAst.Term.Head.Lit(ResolvedAst.Literal.Str("John Doe", SL), SL),
        ResolvedAst.Term.Head.Lit(ResolvedAst.Literal.Int(42, SL), SL)
      ), SL)
    )
    val result = Typer.Constraint.typer(rast, root)
    assert(result.isSuccess)
  }

  test("Constraint.Rule01") {
    val rname = Name.Resolved(List("Edge"))
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("x", SourceLocation.Unknown)
    val z = Name.Ident("x", SourceLocation.Unknown)

    val root = Root.copy(relations = Map(
      rname -> ResolvedAst.Definition.Relation(rname, List(
        ResolvedAst.Attribute(Ident, ResolvedAst.Type.Int),
        ResolvedAst.Attribute(Ident, ResolvedAst.Type.Int),
        ResolvedAst.Attribute(Ident, ResolvedAst.Type.Int)
      ), SL)
    ))

    val head = ResolvedAst.Predicate.Head(rname, List(ResolvedAst.Term.Head.Var(x, SL), ResolvedAst.Term.Head.Var(z, SL)), SL)

    val body = List(
      ResolvedAst.Predicate.Body(rname, List(ResolvedAst.Term.Body.Var(x, SL), ResolvedAst.Term.Body.Var(y, SL)), SL),
      ResolvedAst.Predicate.Body(rname, List(ResolvedAst.Term.Body.Var(y, SL), ResolvedAst.Term.Body.Var(z, SL)), SL)
    )

    val rast = ResolvedAst.Constraint.Rule(head, body)
    val result = Typer.Constraint.typer(rast, root)
    assert(result.isSuccess)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Lattices                                                                //
  /////////////////////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////////////////////////
  // Literals                                                                //
  /////////////////////////////////////////////////////////////////////////////
  test("Literal.Unit") {
    val rast = ResolvedAst.Literal.Unit(SL)
    val result = Typer.Literal.typer(rast, Root)
    assertResult(TypedAst.Type.Unit)(result.tpe)
  }

  test("Literal.Bool.True") {
    val rast = ResolvedAst.Literal.Bool(true, SL)
    val result = Typer.Literal.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.tpe)
  }

  test("Literal.Bool.False") {
    val rast = ResolvedAst.Literal.Bool(false, SL)
    val result = Typer.Literal.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.tpe)
  }

  test("Literal.Int") {
    val rast = ResolvedAst.Literal.Int(42, SL)
    val result = Typer.Literal.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.tpe)
  }

  test("Literal.Str") {
    val rast = ResolvedAst.Literal.Str("foo", SL)
    val result = Typer.Literal.typer(rast, Root)
    assertResult(TypedAst.Type.Str)(result.tpe)
  }

  test("Literal.Tag") {
    val enumName = Name.Resolved(List("foo", "bar", "baz"))
    val tagName = Name.Ident("Qux", SourceLocation.Unknown)
    val literal = ResolvedAst.Literal.Unit(SL)
    val rast = ResolvedAst.Literal.Tag(enumName, tagName, literal, SL)
    val enums = Map(enumName -> ResolvedAst.Definition.Enum(enumName, Map("Qux" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Unit)), SL))
    val root = Root.copy(enums = enums)
    val result = Typer.Literal.typer(rast, root)
    assertResult(TypedAst.Type.Enum(Map("Qux" -> TypedAst.Type.Tag(enumName, tagName, TypedAst.Type.Unit))))(result.tpe)
  }

  test("Literal.Tuple") {
    val rast = ResolvedAst.Literal.Tuple(List(
      ResolvedAst.Literal.Bool(true, SL),
      ResolvedAst.Literal.Int(42, SL),
      ResolvedAst.Literal.Str("foo", SL)), SL)
    val result = Typer.Literal.typer(rast, Root)
    assertResult(TypedAst.Type.Tuple(List(TypedAst.Type.Bool, TypedAst.Type.Int, TypedAst.Type.Str)))(result.tpe)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions                                                             //
  /////////////////////////////////////////////////////////////////////////////
  test("Expression.Var01") {
    val x = Name.Ident("x", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Var(x, SL)
    val env = Map("x" -> TypedAst.Type.Bool)

    val result = Typer.Expression.typer(rast, Root, env)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  test("Expression.Var02") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Binary(
      BinaryOperator.Plus,
      ResolvedAst.Expression.Var(x, SL),
      ResolvedAst.Expression.Var(y, SL)
      , SL)

    val env = Map("x" -> TypedAst.Type.Int, "y" -> TypedAst.Type.Int)

    val result = Typer.Expression.typer(rast, Root, env)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Var03") {
    val x = Name.Ident("x", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Let(
      ident = x,
      value = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL),
      body = ResolvedAst.Expression.Var(x, SL), SL)

    val env = Map("x" -> TypedAst.Type.Bool)

    val result = Typer.Expression.typer(rast, Root, env)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Ref01") {
    val rname = Name.Resolved(List("foo", "bar"))
    val rast = ResolvedAst.Expression.Ref(rname, SL)

    val root = Root.copy(constants = Map(
      rname -> ResolvedAst.Definition.Constant(
        name = rname,
        exp = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
        tpe = ResolvedAst.Type.Bool
        , SL)))

    val result = Typer.Expression.typer(rast, root)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  test("Expression.Ref02") {
    val rname = Name.Resolved(List("foo", "bar"))
    val rast = ResolvedAst.Expression.Ref(rname, SL)

    val root = Root.copy(constants = Map(
      rname -> ResolvedAst.Definition.Constant(
        name = rname,
        exp = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL),
        tpe = ResolvedAst.Type.Int
        , SL)))

    val result = Typer.Expression.typer(rast, root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Lambda01") {
    val x = Name.Ident("x", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Lambda(
      formals = List(ResolvedAst.FormalArg(x, ResolvedAst.Type.Int)),
      retTpe = ResolvedAst.Type.Unit,
      body = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL)
      , SL)

    val expectedType = TypedAst.Type.Lambda(List(TypedAst.Type.Int), TypedAst.Type.Unit)
    val actualType = Typer.Expression.typer(rast, Root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Expression.Lambda02") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)
    val w = Name.Ident("w", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Lambda(
      formals = List(
        ResolvedAst.FormalArg(x, ResolvedAst.Type.Unit),
        ResolvedAst.FormalArg(y, ResolvedAst.Type.Bool),
        ResolvedAst.FormalArg(z, ResolvedAst.Type.Int),
        ResolvedAst.FormalArg(w, ResolvedAst.Type.Str)
      ),
      retTpe = ResolvedAst.Type.Str,
      body = ResolvedAst.Expression.Var(w, SL)
      , SL)

    val expectedType = TypedAst.Type.Lambda(
      args = List(
        TypedAst.Type.Unit,
        TypedAst.Type.Bool,
        TypedAst.Type.Int,
        TypedAst.Type.Str
      ), retTpe = TypedAst.Type.Str)
    val actualType = Typer.Expression.typer(rast, Root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Expression.Lambda.TypeError") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)
    val w = Name.Ident("w", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Lambda(
      formals = List(
        ResolvedAst.FormalArg(x, ResolvedAst.Type.Unit),
        ResolvedAst.FormalArg(y, ResolvedAst.Type.Bool),
        ResolvedAst.FormalArg(z, ResolvedAst.Type.Int),
        ResolvedAst.FormalArg(w, ResolvedAst.Type.Str)
      ),
      retTpe = ResolvedAst.Type.Unit,
      body = ResolvedAst.Expression.Var(w, SL)
      , SL)

    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Apply01") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val rast = ResolvedAst.Expression.Apply(
      lambda =
        ResolvedAst.Expression.Lambda(
          formals = List(ResolvedAst.FormalArg(x, ResolvedAst.Type.Int)),
          retTpe = ResolvedAst.Type.Unit,
          body = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL)
          , SL),
      args = List(ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)), SL)

    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Unit)(result.get.tpe)
  }

  test("Expression.Apply02") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Apply(
      lambda =
        ResolvedAst.Expression.Lambda(
          formals = List(
            ResolvedAst.FormalArg(x, ResolvedAst.Type.Bool),
            ResolvedAst.FormalArg(y, ResolvedAst.Type.Int),
            ResolvedAst.FormalArg(z, ResolvedAst.Type.Str)
          ),
          retTpe = ResolvedAst.Type.Int,
          body = ResolvedAst.Expression.Var(y, SL)
          , SL),
      args = List(
        ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
        ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL),
        ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
      ), SL)

    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Apply.TypeError.IllegalArgumentType") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)

    val rast = ResolvedAst.Expression.Apply(
      lambda =
        ResolvedAst.Expression.Lambda(
          formals = List(
            ResolvedAst.FormalArg(x, ResolvedAst.Type.Bool),
            ResolvedAst.FormalArg(y, ResolvedAst.Type.Int),
            ResolvedAst.FormalArg(z, ResolvedAst.Type.Str)
          ),
          retTpe = ResolvedAst.Type.Int,
          body = ResolvedAst.Expression.Var(y, SL), SL
        ),
      args = List(
        ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL),
        ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL),
        ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
      ), SL)

    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Unary01") {
    val rast = ResolvedAst.Expression.Unary(UnaryOperator.Not, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL), SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  test("Expression.Unary02") {
    val rast = ResolvedAst.Expression.Unary(UnaryOperator.UnaryPlus, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL), SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Unary03") {
    val rast = ResolvedAst.Expression.Unary(UnaryOperator.UnaryMinus, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL), SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Unary.NonBooleanValue") {
    val rast = ResolvedAst.Expression.Unary(UnaryOperator.Not, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL), SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Unary.NonIntegerValue01") {
    val rast = ResolvedAst.Expression.Unary(UnaryOperator.UnaryPlus, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL), SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Unary.NonIntegerValue02") {
    val rast = ResolvedAst.Expression.Unary(UnaryOperator.UnaryMinus, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL), SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Binary01") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(false, SL), SL)
    val rast = ResolvedAst.Expression.Binary(BinaryOperator.And, e1, e2, SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  ignore("Expression.Binary01'") {
    val s = "true && false" // TODO: Consider adding an: asExp

    val result = Compiler.Expression.compile(s)
    assertResult(TypedAst.Type.Bool)(result.tpe)
  }

  test("Expression.Binary02") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(21, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
    val rast = ResolvedAst.Expression.Binary(BinaryOperator.Minus, e1, e2, SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Binary03") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(21, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
    val rast = ResolvedAst.Expression.Binary(BinaryOperator.LessEqual, e1, e2, SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  test("Expression.Binary.MismatchedValues") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
    val rast = ResolvedAst.Expression.Binary(BinaryOperator.Plus, e1, e2, SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Binary.MismatchedOperator") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(false, SL), SL)
    val rast = ResolvedAst.Expression.Binary(BinaryOperator.Plus, e1, e2, SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.IfThenElse01") {
    val rast = ResolvedAst.Expression.IfThenElse(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(21, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.IfThenElse02") {
    val rast = ResolvedAst.Expression.IfThenElse(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(false, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("a", SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("b", SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Str)(result.get.tpe)
  }

  test("Expression.IfThenElse.NonBooleanCondition") {
    val rast = ResolvedAst.Expression.IfThenElse(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(1, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(2, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(3, SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.IfThenElse.ThenElseMismatch") {
    val rast = ResolvedAst.Expression.IfThenElse(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(2, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Let01") {
    val rast = ResolvedAst.Expression.Let(
      Ident,
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(2, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Str)(result.get.tpe)
  }

  test("Expression.Let02") {
    val rast = ResolvedAst.Expression.Let(
      Ident,
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(2, SL), SL),
      ResolvedAst.Expression.Var(Ident, SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Let.TypeError") {
    val rast = ResolvedAst.Expression.Let(
      Ident,
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL),
      ResolvedAst.Expression.Unary(UnaryOperator.Not, ResolvedAst.Expression.Var(Ident, SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Match01") {
    val rast = ResolvedAst.Expression.Match(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      List(
        ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Bool(true, SL), SL) -> ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(21, SL), SL),
        ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Bool(false, SL), SL) -> ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
      ), SL
    )

    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Match02") {
    val rast = ResolvedAst.Expression.Match(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL),
      List(
        ResolvedAst.Pattern.Var(Ident, SL) -> ResolvedAst.Expression.Var(Ident, SL),
        ResolvedAst.Pattern.Var(Ident, SL) -> ResolvedAst.Expression.Var(Ident, SL)
      ), SL
    )

    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Match03") {
    val rast = ResolvedAst.Expression.Match(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Tuple(List(
        ResolvedAst.Literal.Bool(true, SL), ResolvedAst.Literal.Int(42, SL)
      ), SL), SL),
      List(
        ResolvedAst.Pattern.Tuple(List(
          ResolvedAst.Pattern.Wildcard(SourceLocation.Unknown),
          ResolvedAst.Pattern.Var(Ident, SL)
        ), SL) -> ResolvedAst.Expression.Var(Ident, SL)
      ), SL
    )

    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Match.TypeError") {
    val rast = ResolvedAst.Expression.Match(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      List(
        ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Int(42, SL), SL) -> ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL),
        ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Str("foo", SL), SL) -> ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
      ), SL
    )

    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Tag01") {
    val enumName = Name.Resolved(List("Foo", "Bar"))
    val tagName = Name.Ident("Qux", SourceLocation.Unknown)
    val rast = ResolvedAst.Expression.Tag(enumName, tagName, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL), SL)

    val root = Root.copy(enums = Map(
      enumName -> ResolvedAst.Definition.Enum(enumName, Map(
        "Qux" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Unit)
      ), SL)
    ))

    val expectedType = TypedAst.Type.Enum(Map("Qux" -> TypedAst.Type.Tag(enumName, tagName, TypedAst.Type.Unit)))
    val actualType = Typer.Expression.typer(rast, root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Expression.Tag02") {
    val enumName = Name.Resolved(List("Foo", "Bar"))
    val tagName = Name.Ident("C", SourceLocation.Unknown)
    val rast = ResolvedAst.Expression.Tag(enumName, tagName, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL), SL)

    val root = Root.copy(enums = Map(
      enumName -> ResolvedAst.Definition.Enum(enumName, Map(
        "A" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Unit),
        "B" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Bool),
        "C" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Int),
        "D" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Str)
      ), SL)
    ))

    val expectedType = TypedAst.Type.Enum(Map(
      "A" -> TypedAst.Type.Tag(enumName, tagName, TypedAst.Type.Unit),
      "B" -> TypedAst.Type.Tag(enumName, tagName, TypedAst.Type.Bool),
      "C" -> TypedAst.Type.Tag(enumName, tagName, TypedAst.Type.Int),
      "D" -> TypedAst.Type.Tag(enumName, tagName, TypedAst.Type.Str)
    ))
    val actualType = Typer.Expression.typer(rast, root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Expression.Tag.TypeError") {
    val enumName = Name.Resolved(List("Foo", "Bar"))
    val tagName = Name.Ident("A", SourceLocation.Unknown)
    val rast = ResolvedAst.Expression.Tag(enumName, tagName, ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL), SL)

    val root = Root.copy(enums = Map(
      enumName -> ResolvedAst.Definition.Enum(enumName, Map(
        "A" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Unit),
        "B" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Bool),
        "C" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Int),
        "D" -> ResolvedAst.Type.Tag(enumName, tagName, ResolvedAst.Type.Str)
      ), SL)
    ))

    val result = Typer.Expression.typer(rast, root)
    assert(result.isFailure)
  }

  test("Expression.Tuple01") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
    val rast = ResolvedAst.Expression.Tuple(List(e1, e2), SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Tuple(List(TypedAst.Type.Bool, TypedAst.Type.Int)))(result.get.tpe)
  }

  test("Expression.Tuple02") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val e2 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
    val e3 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
    val rast = ResolvedAst.Expression.Tuple(List(e1, e2, e3), SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Tuple(List(TypedAst.Type.Bool, TypedAst.Type.Int, TypedAst.Type.Str)))(result.get.tpe)
  }

  test("Expression.Tuple03") {
    val e1 = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val e2 = ResolvedAst.Expression.Tuple(List(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL)
    ), SL)
    val rast = ResolvedAst.Expression.Tuple(List(e1, e2), SL)
    val result = Typer.Expression.typer(rast, Root)
    val tpe1 = TypedAst.Type.Bool
    val tpe2 = TypedAst.Type.Tuple(List(TypedAst.Type.Unit, TypedAst.Type.Unit))
    assertResult(TypedAst.Type.Tuple(List(tpe1, tpe2)))(result.get.tpe)
  }

  test("Expression.Ascribe01") {
    val rast = ResolvedAst.Expression.Ascribe(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      ResolvedAst.Type.Bool
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Bool)(result.get.tpe)
  }

  test("Expression.Ascribe02") {
    val rast = ResolvedAst.Expression.Ascribe(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      ResolvedAst.Type.Int
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assert(result.isFailure)
  }

  test("Expression.Error01") {
    val rast = ResolvedAst.Expression.IfThenElse(
      ResolvedAst.Expression.Error(ResolvedAst.Type.Bool, SourceLocation.Unknown),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(21, SL), SL),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  test("Expression.Error02") {
    val rast = ResolvedAst.Expression.IfThenElse(
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      ResolvedAst.Expression.Error(ResolvedAst.Type.Int, SourceLocation.Unknown),
      ResolvedAst.Expression.Lit(ResolvedAst.Literal.Int(42, SL), SL)
      , SL)
    val result = Typer.Expression.typer(rast, Root)
    assertResult(TypedAst.Type.Int)(result.get.tpe)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Patterns                                                                //
  /////////////////////////////////////////////////////////////////////////////
  test("Pattern.Wildcard") {
    val rast = ResolvedAst.Pattern.Wildcard(SourceLocation.Unknown)
    val tpe = TypedAst.Type.Bool
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assert(result.isSuccess)
  }

  test("Pattern.Variable01") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val rast = ResolvedAst.Pattern.Var(x, SL)
    val tpe = TypedAst.Type.Bool
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(tpe)(result.get.freeVars(x.name))
  }

  test("Pattern.Variable02") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val rast = ResolvedAst.Pattern.Var(x, SL)
    val tpe = TypedAst.Type.Tuple(List(TypedAst.Type.Bool))
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(tpe)(result.get.freeVars(x.name))
  }

  test("Pattern.Variable03") {
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val rast = ResolvedAst.Pattern.Tuple(List(
      ResolvedAst.Pattern.Var(x, SL),
      ResolvedAst.Pattern.Var(y, SL)
    ), SL)
    val tpe = TypedAst.Type.Tuple(List(TypedAst.Type.Bool, TypedAst.Type.Int))
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(tpe)(result.get.tpe)
  }

  test("Pattern.Literal") {
    val rast = ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    val tpe = TypedAst.Type.Bool
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(tpe)(result.get.tpe)
  }

  test("Pattern.Tag01") {
    val tagName = Name.Ident("Qux", SourceLocation.Unknown)
    val x = Name.Ident("x", SourceLocation.Unknown)
    val rast = ResolvedAst.Pattern.Tag(RName, tagName, ResolvedAst.Pattern.Var(x, SL), SL)
    val tpe = TypedAst.Type.Enum(Map("Qux" -> TypedAst.Type.Tag(RName, tagName, TypedAst.Type.Unit)))
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(TypedAst.Type.Unit)(result.get.freeVars(x.name))
  }

  test("Pattern.Tuple01") {
    val rast = ResolvedAst.Pattern.Tuple(List(
      ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Unit(SL), SL),
      ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Bool(true, SL), SL)
    ), SL)
    val tpe = TypedAst.Type.Tuple(List(
      TypedAst.Type.Unit,
      TypedAst.Type.Bool
    ))
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(tpe)(result.get.tpe)
  }

  test("Pattern.Tuple02") {
    val rast = ResolvedAst.Pattern.Tuple(List(
      ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Unit(SL), SL),
      ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
      ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Int(42, SL), SL),
      ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
    ), SL)

    val tpe = TypedAst.Type.Tuple(List(
      TypedAst.Type.Unit,
      TypedAst.Type.Bool,
      TypedAst.Type.Int,
      TypedAst.Type.Str
    ))
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assertResult(tpe)(result.get.tpe)
  }

  test("Pattern.TypeError") {
    val rast = ResolvedAst.Pattern.Lit(ResolvedAst.Literal.Unit(SL), SL)
    val tpe = TypedAst.Type.Bool
    val result = Typer.Pattern.typer(rast, tpe, Root)
    assert(result.isFailure)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Predicates & Terms                                                      //
  /////////////////////////////////////////////////////////////////////////////
  test("Predicate.Head01") {
    val rname = Name.Resolved(List("foo", "bar"))
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)
    val w = Name.Ident("w", SourceLocation.Unknown)

    val root = Root.copy(relations = Map(
      rname -> ResolvedAst.Definition.Relation(rname, List(
        ResolvedAst.Attribute(x, ResolvedAst.Type.Unit),
        ResolvedAst.Attribute(y, ResolvedAst.Type.Bool),
        ResolvedAst.Attribute(z, ResolvedAst.Type.Int),
        ResolvedAst.Attribute(w, ResolvedAst.Type.Str)
      ), SL)
    ))

    val rast =
      ResolvedAst.Predicate.Head(rname, List(
        ResolvedAst.Term.Head.Lit(ResolvedAst.Literal.Unit(SL), SL),
        ResolvedAst.Term.Head.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
        ResolvedAst.Term.Head.Lit(ResolvedAst.Literal.Int(42, SL), SL),
        ResolvedAst.Term.Head.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
      ), SL)

    val expectedType = TypedAst.Type.Predicate(List(
      TypedAst.Type.Unit, TypedAst.Type.Bool, TypedAst.Type.Int, TypedAst.Type.Str
    ))
    val actualType = Typer.Predicate.typer(rast, root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Predicate.Head02") {
    val rname = Name.Resolved(List("foo", "bar"))
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)
    val w = Name.Ident("w", SourceLocation.Unknown)

    // NB: Somewhat misleading we use the same identifiers for both columns and variables.

    val root = Root.copy(relations = Map(
      rname -> ResolvedAst.Definition.Relation(rname, List(
        ResolvedAst.Attribute(x, ResolvedAst.Type.Unit),
        ResolvedAst.Attribute(y, ResolvedAst.Type.Bool),
        ResolvedAst.Attribute(z, ResolvedAst.Type.Int),
        ResolvedAst.Attribute(w, ResolvedAst.Type.Str)
      ), SL)
    ))

    val rast =
      ResolvedAst.Predicate.Head(rname, List(
        ResolvedAst.Term.Head.Var(x, SL),
        ResolvedAst.Term.Head.Var(y, SL),
        ResolvedAst.Term.Head.Var(z, SL),
        ResolvedAst.Term.Head.Var(w, SL)
      ), SL)

    val expectedType = TypedAst.Type.Predicate(List(
      TypedAst.Type.Unit, TypedAst.Type.Bool, TypedAst.Type.Int, TypedAst.Type.Str
    ))
    val actualType = Typer.Predicate.typer(rast, root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Predicate.Head03") {
    val relationName = Name.Resolved(List("foo", "bar"))
    val functionName = Name.Resolved(List("foo", "baz"))
    val x = Name.Ident("x", SourceLocation.Unknown)

    // NB: Somewhat misleading we use the same identifiers for both columns and variables.

    val root = Root.copy(
      constants = Map(
        functionName -> ResolvedAst.Definition.Constant(
          name = functionName,
          exp = ResolvedAst.Expression.Lambda(
            formals = List(ResolvedAst.FormalArg(x, ResolvedAst.Type.Bool)),
            retTpe = ResolvedAst.Type.Unit,
            body = ResolvedAst.Expression.Lit(ResolvedAst.Literal.Unit(SL), SL), SL
          ),
          tpe = ResolvedAst.Type.Function(List(ResolvedAst.Type.Bool), ResolvedAst.Type.Unit), SL)
      ),
      relations = Map(
        relationName -> ResolvedAst.Definition.Relation(relationName, List(
          ResolvedAst.Attribute(x, ResolvedAst.Type.Unit)
        ), SL)
      ))

    val rast =
      ResolvedAst.Predicate.Head(relationName, List(
        ResolvedAst.Term.Head.Apply(
          functionName,
          List(ResolvedAst.Term.Head.Lit(
            ResolvedAst.Literal.Bool(true, SL), SL)), SL)), SL)

    val expectedType = TypedAst.Type.Predicate(List(TypedAst.Type.Unit))
    val actualType = Typer.Predicate.typer(rast, root).get.tpe
    assertResult(expectedType)(actualType)
  }

  test("Predicate.Body01") {
    val rname = Name.Resolved(List("foo", "bar"))
    val x = Name.Ident("x", SourceLocation.Unknown)
    val y = Name.Ident("y", SourceLocation.Unknown)
    val z = Name.Ident("z", SourceLocation.Unknown)
    val w = Name.Ident("w", SourceLocation.Unknown)

    val root = Root.copy(relations = Map(
      rname -> ResolvedAst.Definition.Relation(rname, List(
        ResolvedAst.Attribute(x, ResolvedAst.Type.Unit),
        ResolvedAst.Attribute(y, ResolvedAst.Type.Bool),
        ResolvedAst.Attribute(z, ResolvedAst.Type.Int),
        ResolvedAst.Attribute(w, ResolvedAst.Type.Str)
      ), SL)
    ))

    val rast =
      ResolvedAst.Predicate.Body(rname, List(
        ResolvedAst.Term.Body.Wildcard(SourceLocation.Unknown),
        ResolvedAst.Term.Body.Lit(ResolvedAst.Literal.Bool(true, SL), SL),
        ResolvedAst.Term.Body.Lit(ResolvedAst.Literal.Int(42, SL), SL),
        ResolvedAst.Term.Body.Lit(ResolvedAst.Literal.Str("foo", SL), SL)
      ), SL)

    val expectedType = TypedAst.Type.Predicate(List(
      TypedAst.Type.Unit, TypedAst.Type.Bool, TypedAst.Type.Int, TypedAst.Type.Str
    ))
    val actualType = Typer.Predicate.typer(rast, root).get.tpe
    assertResult(expectedType)(actualType)
  }

  // TODO: Test Term.Ascribe.


  /////////////////////////////////////////////////////////////////////////////
  // Types                                                                   //
  /////////////////////////////////////////////////////////////////////////////
  test("Type.Unit") {
    val rast = ResolvedAst.Type.Unit
    val result = Typer.Type.typer(rast)
    assertResult(TypedAst.Type.Unit)(result)
  }

  test("Type.Bool") {
    val rast = ResolvedAst.Type.Bool
    val result = Typer.Type.typer(rast)
    assertResult(TypedAst.Type.Bool)(result)
  }

  test("Type.Int") {
    val rast = ResolvedAst.Type.Int
    val result = Typer.Type.typer(rast)
    assertResult(TypedAst.Type.Int)(result)
  }

  test("Type.Str") {
    val rast = ResolvedAst.Type.Str
    val result = Typer.Type.typer(rast)
    assertResult(TypedAst.Type.Str)(result)
  }

  test("Type.Tag") {
    val rast = ResolvedAst.Type.Tag(RName, Ident, ResolvedAst.Type.Unit)
    val result = Typer.Type.typer(rast)
    assertResult(TypedAst.Type.Tag(RName, Ident, TypedAst.Type.Unit))(result)
  }

  test("Type.Tuple") {
    val rtype1 = ResolvedAst.Type.Bool
    val rtype2 = ResolvedAst.Type.Int
    val rast = ResolvedAst.Type.Tuple(List(rtype1, rtype2))

    val tpe1 = TypedAst.Type.Bool
    val tpe2 = TypedAst.Type.Int
    val result = Typer.Type.typer(rast)

    assertResult(TypedAst.Type.Tuple(List(tpe1, tpe2)))(result)
  }

  test("Type.Function") {
    val rast = ResolvedAst.Type.Function(List(ResolvedAst.Type.Bool, ResolvedAst.Type.Int), ResolvedAst.Type.Str)
    val tast = TypedAst.Type.Lambda(List(TypedAst.Type.Bool, TypedAst.Type.Int), TypedAst.Type.Str)

    val result = Typer.Type.typer(rast)
    assertResult(tast)(result)
  }

}
