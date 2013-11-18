var Assert = {
		
	assertEquals : function(obj1, obj2, msg) {
		if (obj1 != obj2) {
			throw msg;
		}
	},

	assertEquals : function(obj1, obj2) {
		assertEquals(obj1, obj2, obj2 + " expected but was " + obj1);
	}
	
};