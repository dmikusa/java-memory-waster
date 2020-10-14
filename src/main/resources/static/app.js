var alert = new Vue({
	el: '#alert',
	data: function () {
		return {
			message: "",
			type: "",
		};
	},
	computed: {
		show: function () {
			return this.message != "" && this.type != "";
		}
	},
	methods: {
		clear: function () {
			this.message = "";
			this.type = "";
		}
	}
});

function newStartStopPanel(id, api) {
	return new Vue({
		el: '#' + id,
		methods: {
			initiate: function (event) {
				axios.post(api, null, {
					config: { timeout: 2 },
				})
					.then(() => {
						alert.message = "Success";
						alert.type = "success";
						setTimeout(() => alert.clear(), 2000);
					})
					.catch(err => {
						alert.message = err.toString();
						alert.type = "danger";
						console.error(err);
					});
			},
			interrupt: function (event) {
				axios.delete(api, null, {
					config: { timeout: 2 },
				})
					.then(() => {
						alert.message = "Success";
						alert.type = "success";
						setTimeout(() => alert.clear(), 2000);
					})
					.catch(err => {
						alert.message = err.toString();
						alert.type = "danger";
						console.error(err);
					});
			},
		}
	});
}

function newBasicPanel(id, api, defaultHowMuch) {
	return new Vue({
		el: '#' + id,
		data: function () {
			return {
				howMuch: (defaultHowMuch) ? defaultHowMuch : 1000,
				retain: false,
			};
		},
		methods: {
			click: function (event) {
				var params = {};
				if (id != "gc") {
					params["howMuch"] = this.howMuch;
					if (id != "threads") {
						params["retain"] = this.retain;
					}
				}
				axios
					.post(api, null, {
						params: params,
						config: { timeout: 2 },
					})
					.then(() => {
						alert.message = "Success";
						alert.type = "success";
						setTimeout(() => alert.clear(), 2000);
					})
					.catch(err => {
						alert.message = err.toString();
						alert.type = "danger";
						console.error(err);
					});
			}
		}
	});
}

var heapBtn = newBasicPanel("heap", "/api/v1/memory/heap");
var metaBtn = newBasicPanel("metaspace", "/api/v1/memory/metaspace");
var threadsBtn = newBasicPanel("threads", "/api/v1/threads", 10);
var gcBtn = newBasicPanel("gc", "/api/v1/memory/gc");

var deadlock = newStartStopPanel("deadlock", "/api/v1/deadlock");
var slowThreads = newStartStopPanel("slowthreads", "/api/v1/slow-threads");