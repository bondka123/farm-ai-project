import mysql.connector

db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="root123",
    database="attendance_db"
)

cursor = db.cursor()