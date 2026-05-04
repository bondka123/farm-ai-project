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
    const role = localStorage.getItem('role');

    // Force face setup for Manager/Viewer
    if ((role === 'ROLE_MANAGER' || role === 'ROLE_VIEWER') && faceRegistered !== 'true') {
      return this.router.createUrlTree(['/face-setup']);
    }

    // Protect routes based on role metadata
    if (route.data['role'] && role !== route.data['role']) {
      if (role === 'ROLE_MANAGER') {
        return this.router.createUrlTree(['/dashboard/manager']);
      }
      if (role === 'ROLE_VIEWER') {
        return this.router.createUrlTree(['/dashboard/viewer']);
      }
      if (role === 'ROLE_ADMIN') {
        return this.router.createUrlTree(['/dashboard']);
      }
    }

    return true;
  }
}
