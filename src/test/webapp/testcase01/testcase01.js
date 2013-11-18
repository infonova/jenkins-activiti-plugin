// TODO: use JsMockito

$(function() {
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
			var action1 = new WorkflowAction("id1");
			var action2 = new WorkflowAction("id2");
			var action3 = new WorkflowAction("id3");
			return [ action1, action2, action3 ];
		}
		callback(response);
	}

	// mock getHighlightElements function
	it.getHighlightElements = function(processDescriptionId, buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			var area1 = new EventArea("1", "SUCCESS", "23", "23", "18");
			var area2 = new TaskArea("2", "SUCCESS", "95", "75", "200", "130");
			var area3 = new TaskArea("3", "SUCCESS", "295", "175", "400", "230");
			var area4 = new EventArea("4", "SUCCESS", "563", "203", "18");
			return [ area1, area2, area3, area4 ];
		}
		callback(response);
	}
	
	// mock async function
	it.async = function(callback) {
		callback();
	}

	JenkinsActivitiPlugin.prototype.getImageURL = function() {
		return getProcessDefinitionId() + ".png";
	}

	JenkinsActivitiPlugin.prototype.it = it;
});

/**
 * ****************************
 * Test definition
 * ****************************
 */
test("test simple build execution", function() {
	var divMap = $("div .map");
	var divMapExists = divMap != undefined;
	ok(divMapExists == true, "map initialization expected");
	
	var areas = $("[name='map']").children().length;
	ok(areas == "4", "4 Areas expected");
	
	var dialogs = $(".ui-dialog").length;
	ok(dialogs == "4", "4 Dialogs expected");
});