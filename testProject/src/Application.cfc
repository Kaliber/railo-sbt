component {

  this.name = "testit2";
  
  public boolean function onApplicationStart() {
    var out = createObject("java", "java.lang.System").out;
    
    out.println("Loading configuration");
    var configFactory = createObject("java", "com.typesafe.config.ConfigFactory");
    configFactory.invalidateCaches();
    
    var config = configFactory.load();
    
    application["config"] = config.getObject("railo").unwrapped();
    
    return true;
  }
}