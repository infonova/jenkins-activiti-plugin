// TODO: use JsMockito

// store display() function in order to hook the method
var defaultDisplay = undefined;

$(function() {
	defaultDisplay = display;
	
	var it = new Object();

	// mock getErrorAction function
	it.getErrorAction = function(buildNr, callback) {
		var response = new Object();
		response.responseObject = nullAnswer;
		callback(response);
	}

	// mock isBuilding function
	it.isBuilding = function(buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			return false;
		};
		callback(response);
	}

	// mock getProcessDefinitionIds function
	it.getProcessDefinitionIds = function(buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			console.debug(buildNr);
			if (buildNr == "build1") {
				return [ new WorkflowAction("diagram1") ];
			} else if (buildNr == "build2") {
				return [ new WorkflowAction("diagram2") ];
			}
		}
		callback(response);
	}

	// mock getHighlightElements function
	it.getHighlightElements = function(processDescriptionId, buildNr, callback) {
		var response = new Object();

		console.debug(buildNr);
		if (buildNr == "build1") {
			response.responseObject = buildCallback1;
		} else if (buildNr == "build2") {
			response.responseObject = buildCallback2;
		}
		callback(response);
	}

	// mock async function
	it.async = function(callback) {
		callback();
	}

	// mock getTaskStates function
	it.getTaskStates = function(processDefinitionId, buildNr, callback) {
		var response = new Object();

		var map = {};
		map[1] = "SUCCESS";
		map[2] = "SUCCESS";
		map[3] = "SUCCESS";
		map[4] = "SUCCESS";
		map[5] = "SUCCESS";
		map[6] = "SUCCESS";
		map[7] = "SUCCESS";

		response.responseObject = function() {
			return map;
		}

		callback(response);
	}

	JenkinsActivitiPlugin.prototype.getImageURL = function() {
		return getProcessDefinitionId() + ".png";
	}

	JenkinsActivitiPlugin.prototype.it = it;
});

function buildCallback1() {
	var area1 = new EventArea("1", "SUCCESS", "23", "23", "18");
	var area2 = new TaskArea("2", "SUCCESS", "95", "75", "200", "130");
	var area3 = new TaskArea("3", "SUCCESS", "295", "175", "400", "230");
	var area4 = new EventArea("4", "SUCCESS", "563", "203", "18");
	return [ area1, area2, area3, area4 ];
}

function buildCallback2() {
	var area1 = new EventArea("1", "SUCCESS", "23", "153", "18");
	var area2 = new TaskArea("2", "SUCCESS", "85", "125", "190", "180");
	var area3 = new TaskArea("3", "SUCCESS", "305", "5", "410", "60");
	var area4 = new TaskArea("4", "SUCCESS", "325", "215", "430", "270");
	var area5 = new GatewayArea("5", "SUCCESS", "245", "152", "265", "132",
			"285", "152", "265", "172");
	var area6 = new GatewayArea("6", "SUCCESS", "525", "152", "545", "132",
			"565", "152", "545", "172");
	var area7 = new EventArea("7", "SUCCESS", "628", "153", "18");
	return [ area1, area2, area3, area4, area5, area6, area7 ];
}

/**
 * **************************** Test definition ****************************
 */

test("test build click navigation", function() {
	
	// test default build
	var divMap = $("div .map");
	var divMapExists = divMap != undefined;
	ok(divMapExists == true, "map initialization expected");

	var areas = $("[name='map']").children().length;
	ok(areas == "4", "4 Areas expected");

	var dialogs = $(".ui-dialog").length;
	ok(dialogs == "4", "4 Dialogs expected");
	
	// test frontend after click on tab navigation
	$("li #build2").click();
	
	
	var that = this;
	
	// add display() hook
	display = function() {
		defaultDisplay();
		
		var areas = $("[name='map']").children().length;
		Assert.assertEquals(areas, 7, "7 areas expected");
		assertEquals(areas, 7, "7 areas expected");
	}
	
	$("select #build1").click();
	
	// add display() hook
	display = function() {
		defaultDisplay();
		
		var areas = $("[name='map']").children().length;
		assertEquals(areas, 4, "4 areas expected");
	}
	
});
