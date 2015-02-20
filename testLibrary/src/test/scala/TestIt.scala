import nl.rhinofly.railosbt.RailoRunner
import org.qirx.littlespec.Specification
import lucee.runtime.PageContext

object TestIt extends Specification {

  "RailoRunner should be able to call CFC's from Scala, code:" - example {
    class Test()(implicit pc:PageContext) {
      val cfc = pc.loadComponent("Test")
      def test = cfc.call(pc, "test", Array.empty[AnyRef])
    }
    
    RailoRunner.withRailo(9191) { (config, pageContext) =>
      implicit val pc = pageContext
      val testComponent = new Test()
      testComponent.test is "tests-fromScala"
    }
  }
  
  "Fancy wrapper for railo interaction, code:" - example {
    def withRailo[T](code: PageContext => T):T = 
      RailoRunner.withRailo(9191)( (_, p) => code(p))
      
    import scala.language.dynamics
    case class Cfc(component:String)(implicit pc:PageContext) extends Dynamic {
      private val cfc = pc.loadComponent(component)
      def applyDynamic(method:String)(arguments: AnyRef *) = 
        cfc.call(pc, method, arguments.toArray)
    }
    
    withRailo { implicit pc =>
      val testComponent = Cfc("Test")
      val result = testComponent.test()
      result is "tests-fromScala"
    }
  }
}