<cfdump var="#application#" />

<cfset t = new testLibrary.Test() />
<cfdump var="#t.test()#" />

<cfset t2= new testLibrary.test2.Test2() />
<cfdump var="#t2.test2()#" />

<cfinclude template="/testLibrary/test2/testFile2.cfm" />

<cfdump var="#server#" />
