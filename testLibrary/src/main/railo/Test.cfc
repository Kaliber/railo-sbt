component {
  
  variables.message = "tests-"
  
  function init() {
    variables.message = "tests-" & "init-"
    return this;
  }

  function test() {
    return variables.message & createObject("java", "test.FromScala").test();
  }
}