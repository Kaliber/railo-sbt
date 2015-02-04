<cfdump var="#application#" />

<cfset t = new test.Test() />
<cfdump var="#t.test()#" />

<cfset t2= new test.test2.Test2() />
<cfdump var="#t.test2()#" />

<cfdump var="#server#" />
