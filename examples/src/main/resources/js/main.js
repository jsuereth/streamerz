
$(document).ready(function() {
	var path = "ws://localhost:8080/"
	var socket = new WebSocket(path)
	socket.onmessage = function (msg) {
      $('body').html(msg.data);
    }
	
})