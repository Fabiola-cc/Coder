// ============================================
// SUITE DE EJEMPLOS COMPLETA - COMPISCRIPT
// Demostración de todas las características
// ============================================

// ==========================================
// 1. DECLARACIÓN, OPERACIONES Y PRINT
// ==========================================
print("=== 1. Declaraciones y Operaciones ===\n");
let a: integer = 5;
let b: integer = 7;
let c: integer = a + b;
print("a + b = ");
print(c);
print("\n");

let d: integer = a * b;
print("a * b = ");
print(d);
print("\n\n");

// ==========================================
// 2. FUNCIONES Y RETORNO
// ==========================================
print("=== 2. Funciones ===\n");

function cuadrado(x: integer): integer {
    return x * x;
}

function doble(n: integer): integer {
    return n * 2;
}

let r: integer = cuadrado(6);
print("cuadrado(6) = ");
print(r);
print("\n");

let x: integer = doble(5);
print("doble(5) = ");
print(x);
print("\n\n");

// ==========================================
// 3. CLASES Y MÉTODOS
// ==========================================
print("=== 3. Clases y Metodos ===\n");

class MathOps {
    function suma(a: integer, b: integer): integer {
        return a + b;
    }
}

let m: MathOps = new MathOps();
let result: integer = m.suma(3, 4);
print("3 + 4 = ");
print(result);
print("\n\n");

// ==========================================
// 4. CLASE CON CONSTRUCTOR Y PROPIEDADES
// ==========================================
print("=== 4. Clase con Constructor ===\n");

class Persona {
    let nombre: string;
    let edad: integer;

    function constructor(nombre: string, edad: integer) {
        this.nombre = nombre;
        this.edad = edad;
    }

    function saludar(): string {
        return "Hola " + this.nombre;
    }
}

let p: Persona = new Persona("Juan", 25);
print("Edad de Juan: ");
print(p.edad);
print("\n\n");

// ==========================================
// 5. CICLO FOR
// ==========================================
print("=== 5. Ciclo FOR ===\n");
for (let i: integer = 0; i < 3; i = i + 1) {
    print("Loop index: " + i);
    print("\n");
}
print("\n");

// ==========================================
// 6. CICLO WHILE CON IF
// ==========================================
print("=== 6. IF + WHILE ===\n");

let n: integer = 5;
if (n > 0) {
let i: integer = 1;
while (i <= n) {
     print(i);
     print("\n")
     i = i + 1;
}
} else {
print(0);
}
print("\n");

// ==========================================
// 7. FOREACH
// ==========================================
print("=== 7. FOREACH ===\n");
let nums: integer[] = [1, 2, 3];
foreach (valor in nums) {
    print(valor);
    print(" ");
}
print("\n\n");

// ==========================================
// 8. ARRAYS Y MATRICES
// ==========================================
print("=== 8. Arrays y Matrices ===\n");

let arr: integer[] = [10, 20, 30];

let idx: integer = 2;
let valB: integer = arr[idx];
print("arr[2] = ");
print(valB);
print("\n");

arr[0] = 99;
let valC: integer = arr[0];
print("arr[0] despues de asignar 99 = ");
print(valC);
print("\n");

// ==========================================
// 9. SWITCH
// ==========================================
print("=== 9. SWITCH ===\n");
let switchVal: integer = 2;
switch (switchVal) {
    case 1:
        print("Uno\n");
        break;
    case 2:
        print("Dos\n");
        break;
    default:
        print("Otro\n");
}
print("\n");

// ==========================================
// 10. BREAK Y CONTINUE
// ==========================================
print("=== 10. Break y Continue ===\n");
let loopI: integer = 0;
while (loopI < 5) {
    loopI = loopI + 1;
    if (loopI == 2) {
        continue;
    }
    if (loopI == 4) {
        break;
    }
    print(loopI);
    print(" ");
}
print("\n\n");

// ==========================================
// 11. OPERADORES LÓGICOS
// ==========================================
print("=== 11. Operadores Logicos ===\n");

let boolA: boolean = true;
let boolB: boolean = false;
if (boolA && !boolB) {
    print("AND test: OK\n");
} else {
    print("AND test: NOP\n");
}

let boolC: boolean = false;
let boolD: boolean = true;
if (boolC || !boolD) {
    print("OR test: OK\n");
} else {
    print("OR test: NOP\n");
}
print("\n");

// ==========================================
// 12. OPERADOR TERNARIO
// ==========================================
print("=== 12. Operador Ternario ===\n");
let X: integer = 8;
let Y: integer = (X > 5) ? 100 : 0;
print("(8 > 5) ? 100 : 0 = ");
print(Y);
print("\n\n");

// ==========================================
// 13. FUNCIONES CON ESTRUCTURAS DE CONTROL
// ==========================================
print("=== 13. Funcion con IF y WHILE ===\n");

function makeAdder(x: integer): integer {
    return x + 2;
}

let addVal: integer = makeAdder(3);
print("Valor inicial: ");
print(addVal);
print("\n");

while (addVal < 7) {
    if (addVal > 5) {
        print("Mayor que 5\n");
    } else {
        print("5 o menos\n");
    }
    addVal = addVal + 1;
}
print("\n");

// ==========================================
// 14. RECURSIVIDAD - FACTORIAL
// ==========================================
print("=== 14. Recursividad - Factorial ===\n");

function factorial(n: integer): integer {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}

let factResult: integer = factorial(5);
print("factorial(5) = ");
print(factResult);
print("\n\n");

// ==========================================
// 15. RECURSIVIDAD - FIBONACCI
// ==========================================
print("=== 15. Recursividad - Fibonacci ===\n");

function fibonacci(n: integer): integer {
    if (n <= 1) {
        return n;
    }
    let fibA: integer = fibonacci(n - 1);
    let fibB: integer = fibonacci(n - 2);
    return fibA + fibB;
}

let fibResult: integer = fibonacci(10);
print("fibonacci(10) = ");
print(fibResult);
print("\n\n");

// ==========================================
// FIN DE LA SUITE DE EJEMPLOS
// ==========================================
print("=== SUITE DE EJEMPLOS COMPLETADA ===\n");