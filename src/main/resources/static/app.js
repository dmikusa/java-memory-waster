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

var deadlock = new Vue({
	el: '#deadlock',
	methods: {
		initiate: function (event) {
			axios.post("/api/v1/deadlock", null, {
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
			axios.delete("/api/v1/deadlock", null, {
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

function newButton(id, api, defaultHowMuch) {
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

var heapBtn = newButton("heap", "/api/v1/memory/heap");
var metaBtn = newButton("metaspace", "/api/v1/memory/metaspace");
var threadsBtn = newButton("threads", "/api/v1/threads", 10);
var gcBtn = newButton("gc", "/api/v1/memory/gc");
