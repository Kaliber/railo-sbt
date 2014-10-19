component {
  function test() {
    return "tests-" & createObject("java", "test.FromScala").test();
  }
}