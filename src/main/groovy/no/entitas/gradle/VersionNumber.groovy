package no.entitas.gradle

class VersionNumber implements Comparable{
	String branchName
	BigDecimal version
	BigDecimal nextVersion 
	
	public VersionNumber(String tagName){
		 def tagNameParts = tagName.split('-')
	     version =  new BigDecimal(tagNameParts[-1])
		 nextVersion = version.add(BigDecimal.ONE)
	 	 branchName = tagNameParts[0]
	}
	
	def nextVersionTag(){
		return "$branchName-REL-$nextVersion"
	}
	
	def String toString(){
		return "$branchName-REL-$version"
	}
	
	int compareTo(other) { version.compareTo(other.version) } 
}