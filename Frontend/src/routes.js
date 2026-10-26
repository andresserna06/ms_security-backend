import React from 'react'

// Dashboard
const Dashboard = React.lazy(() => import('./views/dashboard/Dashboard'))

// Administración y Seguridad
const UsersPage = React.lazy(() => import('./views/users/UsersPage'))

export const routes = [
  { path: '/', exact: true, name: 'Inicio' },
  { path: '/dashboard', name: 'Dashboard', element: Dashboard },
  { path: '/users', name: 'Usuarios', element: UsersPage },
]

export default routes
