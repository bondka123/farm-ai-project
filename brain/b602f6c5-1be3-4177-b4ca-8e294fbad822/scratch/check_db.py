import mysql.connector
import json

def get_db():
    return mysql.connector.connect(
        host="localhost",
        user="root",
        password="root123",
        database="attendance_db"
    )

def check_data():
    try:
        db = get_db()
        cursor = db.cursor()
        
        print("--- EMPLOYEES ---")
        cursor.execute("SELECT id, name, email, face_registered FROM employees")
        for row in cursor.fetchall():
            print(row)
            
        print("\n--- USERS ---")
        cursor.execute("SELECT email, role, face_registered FROM users")
        for row in cursor.fetchall():
            print(row)
            
        cursor.close()
        db.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_data()
