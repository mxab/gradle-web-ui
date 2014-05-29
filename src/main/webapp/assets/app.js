var application = angular.module('ui.bootstrap.demo',
		[ 'ui.bootstrap', 'ngTouch' ], function($httpProvider) {

		}).run([ '$location', function($location) {

} ]);

/**
 * @ngInject
 * @param $scope
 * @param $http
 */
function MainController($scope, $http) {

	this.$scope = $scope
	this.$http = $http;
	var appPrefix = "";
	var socket = new SockJS('/app');

	this.stompClient = Stomp.over(socket);

	$scope.runningTasks = {};
	this.stompClient.connect({}, function(frame) {

		this.stompClient.connect()
		console.log('Connected: ' + frame);
		/**
		 * @type {string}
		 */
		var subscribeTo = appPrefix + '/topic/tasks/progress'

		this.stompClient.subscribe(subscribeTo, function(message) {
			// console.log("message", message);
			var data = JSON.parse(message.body);
			var task = this.$scope.runningTasks[data.taskId];
			if (task) {

				this.$scope.$apply(function() {
					task.progress.push(data.description);
				})
			} else {
				console.warn("Received progress for unkown task", data);
			}

		}.bind(this), {});
	}.bind(this), function() {
		console.log(arguments);
	}.bind(this));

	$http.get("/tasks").then(function(tasks) {
		$scope.tasks = tasks.data;
	});
};
MainController.prototype.runTask = function(task) {
	this.$http.post("/tasks/" + task).then(function(result) {

		var data = result.data;
		this.$scope.runningTasks[data.id] = {
			progress : []
		}

	}.bind(this));
};
MainController.prototype.removeRunningTask = function(id) {
	delete this.$scope.runningTasks[id]
};
MainController.prototype.formatProgress = function(progress) {
	return _(progress).join("\n");
};