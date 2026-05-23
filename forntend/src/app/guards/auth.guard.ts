import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    const token = localStorage.getItem('token');
    
    if (!token) {
      return this.router.createUrlTree(['/login']);
    }

    const faceRegistered = localStorage.getItem('faceRegistered');
    const rawRole = localStorage.getItem('role') || '';
    const role = this.normalizeRole(rawRole);

    // Force face setup for Manager/Viewer
    if ((role === 'ROLE_MANAGER' || role === 'ROLE_VIEWER') && faceRegistered !== 'true') {
      return this.router.createUrlTree(['/face-setup']);
    }

    // Protect routes based on role metadata
    const requiredRole = this.normalizeRole(route.data['role']);

    if (requiredRole && role !== requiredRole) {
      console.warn(`Access Denied. Required: ${requiredRole}, Found: ${role}`);
      
      if (role === 'ROLE_MANAGER') {
        return this.router.createUrlTree(['/dashboard/manager']);
      }
      if (role === 'ROLE_VIEWER') {
        return this.router.createUrlTree(['/dashboard/viewer']);
      }
      if (role === 'ROLE_ADMIN') {
        return this.router.createUrlTree(['/dashboard']);
      }
      return this.router.createUrlTree(['/login']);
    }

    return true;
  }

  private normalizeRole(role: string): string {
    if (!role) return '';
    const r = role.toUpperCase();
    if (r.includes('ADMIN')) return 'ROLE_ADMIN';
    if (r.includes('MANAGER')) return 'ROLE_MANAGER';
    if (r.includes('VIEWER') || r.includes('OBSERVER')) return 'ROLE_VIEWER';
    return r;
  }
}
