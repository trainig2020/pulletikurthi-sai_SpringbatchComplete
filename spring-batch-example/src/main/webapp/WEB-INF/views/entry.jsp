<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Schedule Form</title>
</head>
<body>
	<h1>Hello Welcome to manual and auto schedule script running form</h1>


	<a href="/load">AutoScheduling</a>
	<br>
	<br>

	<h3>File Upload:</h3>
	Select a file to upload:
	<br>
	<form action="/load/fileupload" method="post" enctype="multipart/form-data">
	
		<input type="file" name="fileUpload" size="50" /> <br> 
		<input type="submit" value="Upload File" />
		<!-- <button type="submit" class="btn btn-primary" >Upload File</button> -->
		
	</form>
	
	<br><br>

	<form action="/load/manualmode" method="post">

		<input type="datetime-local" name="selecteddate"
			value="2020-10-12 14:52:00" step="1"></br> </br>

		<c:forEach items="${ListOfFiles}" var="list">
			<input type="checkbox" name="names" value="${list}">
			</br>
			<c:out value="${list}"></c:out>
			</br>
		</c:forEach>

		<input type="submit" value="Submit">

	</form>
</body>
</html>