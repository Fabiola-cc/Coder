// Caso 1: Array global con índice constante
let arr: integer[] = [10, 20, 30];
print(arr[1]);  // Debería imprimir 20

print("\n");

// Caso 2: Array con índice variable
let i: integer = 2;
print(arr[i]);  // Debería imprimir 30

print("\n");

// Caso 3: Asignación
arr[0] = 99;
print(arr[0]);  // Debería imprimir 99

print("\n");