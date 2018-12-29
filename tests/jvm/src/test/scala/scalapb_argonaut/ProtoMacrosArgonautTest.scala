package scalapb_json

import org.scalatest.FunSpec
import org.scalatest.Matchers
import scalapb_argonaut.ProtoMacrosArgonaut._
import com.google.protobuf.struct._
import jsontest.test.MyTest

import scala.util.Success

class ProtoMacrosArgonautTest extends FunSpec with Matchers {
  describe("ProtoMacrosArgonaut") {
    it("struct") {
      assert(struct"{}" == Struct.defaultInstance)

      assert(
        struct"""{ "a" : [1, false, null, "x"] }""" == Struct(
          Map(
            "a" -> Value(
              Value.Kind.ListValue(
                ListValue(
                  List(
                    Value(Value.Kind.NumberValue(1.0)),
                    Value(Value.Kind.BoolValue(false)),
                    Value(Value.Kind.NullValue(NullValue.NULL_VALUE)),
                    Value(Value.Kind.StringValue("x"))
                  )
                )
              )
            )
          )
        )
      )

      """ struct" { invalid json " """ shouldNot compile
    }

    it("value") {
      assert(value" 42 " == Value(Value.Kind.NumberValue(42)))
      assert(value" false " == Value(Value.Kind.BoolValue(false)))
      assert(value""" "x" """ == Value(Value.Kind.StringValue("x")))
      assert(value""" [] """ == Value(Value.Kind.ListValue(ListValue())))

      """ value" { invalid json " """ shouldNot compile
    }

    it("fromJson") {
      assert(MyTest.fromJsonConstant("{}") === MyTest())
      assert(
        MyTest.fromJsonConstant(
          """{
            "hello" : "foo",
            "foobar" : 42
          }"""
        ) === MyTest().update(
          _.hello := "foo",
          _.foobar := 42
        )
      )
      """ jsontest.test.MyTest.fromJsonConstant("{") """ shouldNot compile

      assert(MyTest.fromJsonTry("{").isFailure)
      assert(MyTest.fromJsonTry("""{"hello":"foo"}""") === Success(MyTest(hello = Some("foo"))))

      assert(MyTest.fromJsonOpt("{").isEmpty)
      assert(MyTest.fromJsonOpt("""{"hello":"foo"}""") === Some(MyTest(hello = Some("foo"))))

      assert(MyTest.fromJsonEither("{").isLeft)
      assert(MyTest.fromJsonEither("""{"hello":"foo"}""") === Right(MyTest(hello = Some("foo"))))
    }
  }
}
