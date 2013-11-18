/**
 * Dummy function which returns null.
 * 
 * @returns null
 */
function nullAnswer() {
	return null;
}

function EventArea(id, state, x, y, radius) {
	this.activityId = id;
	this.state = state;
	this.x = x;
	this.y = y;
	this.radius = radius;
	this.link = "#" + id;
	this.shape = "CIRCLE";
	this.activityType = "EVENT";
	this.coordinates = function() {
		return x + "," + y + "," + radius;
	}
}

function TaskArea(id, state, x1, y1, x2, y2) {
	this.activityId = id;
	this.state = state;
	this.x1 = x1;
	this.y1 = y1;
	this.x2 = x2;
	this.y2 = y2;
	this.link = "#" + id;
	this.shape = "RECTANGLE";
	this.activityType = "TASK";
	this.coordinates = function() {
		return x1 + "," + y1 + "," + x2 + "," + y2;
	}
}

function GatewayArea(id, state, x1, y1, x2, y2, x3, y3, x4, y4) {
	this.activityId = id;
	this.state = state;
	this.x1 = x1;
	this.y1 = y1;
	this.x2 = x2;
	this.y2 = y2;
	this.x3 = x3;
	this.y3 = y3;
	this.x4 = x4;
	this.y4 = y4;
	this.link = "#" + id;
	this.shape = "POLY";
	this.activityType = "GATEWAY";
	this.coordinates = function() {
		return x1 + "," + y1 + "," + x2 + "," + y2 + "," + x3 + "," + y3 + "," + x4 + "," + y4;
	}
}

function WorkflowAction(processDescriptionId) {
	this.processDescriptionId = processDescriptionId;
}

function assertEquals(obj1, obj2, msg) {
	if (obj1 != obj2) {
		throw msg;
	}
}

function assertEquals(obj1, obj2) {
	assertEquals(obj1, obj2, obj2 + " expected but was " + obj1);
}
