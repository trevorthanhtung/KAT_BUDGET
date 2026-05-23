import java.io.File  
import javax.xml.parsers.DocumentBuilderFactory  
val doc1 = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(\" "app/src/main/res/values/strings.xml\))  
val vn = doc1.getElementsByTagName(\string\); val vnSet = mutableSetOf<String>()  
for(i in 0 until vn.length) vnSet.add(vn.item(i).attributes.getNamedItem(\name\).nodeValue)  
val doc2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(\app/src/main/res/values-en/strings.xml\))  
val en = doc2.getElementsByTagName(\string\); val enSet = mutableSetOf<String>()  
for(i in 0 until en.length) enSet.add(en.item(i).attributes.getNamedItem(\name\).nodeValue)  
println(\VN" - EN: "\ + (vnSet - enSet))  
println(\EN" - VN: "\ + (enSet - vnSet))  
