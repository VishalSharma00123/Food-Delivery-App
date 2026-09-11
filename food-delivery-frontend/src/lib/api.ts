const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface ApiError {
  status: number;
  message: string;
  details?: unknown;
  path?: string;
}

function getStoredToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const token = localStorage.getItem('token');
  if (!token || token.trim().length === 0 || token === 'undefined' || token === 'null') {
    return null;
  }
  return token.trim();
}

class ApiClient {
  private getHeaders(secured: boolean = true): HeadersInit {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    };

    if (secured) {
      const token = getStoredToken();
      if (!token) {
        const apiError: ApiError = {
          status: 401,
          message: 'Please sign in again — auth token is missing',
        };
        throw apiError;
      }
      headers.Authorization = `Bearer ${token}`;
    }

    return headers;
  }

  private async handleResponse<T>(response: Response, path?: string): Promise<T> {
    if (!response.ok) {
      let errorMessage = 'An unexpected error occurred';
      let details: unknown = null;

      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorData.error || errorMessage;
        details = errorData;
      } catch {
        try {
          errorMessage = (await response.text()) || response.statusText;
        } catch {
          errorMessage = response.statusText;
        }
      }

      const apiError: ApiError = {
        status: response.status,
        message: errorMessage,
        details,
        path,
      };

      // Clear session only when the token itself is rejected.
      // Do NOT treat downstream "Authentication required" as logout — that can
      // mean a service missed identity headers while the JWT is still valid.
      if (
        response.status === 401 &&
        typeof window !== 'undefined' &&
        /invalid or expired|please sign in again/i.test(errorMessage)
      ) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('bitecraft:auth-lost'));
      }

      throw apiError;
    }

    if (response.status === 204) {
      return {} as T;
    }

    try {
      return (await response.json()) as T;
    } catch {
      return {} as T;
    }
  }

  async get<T>(path: string, secured = true): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'GET',
      headers: this.getHeaders(secured),
    });
    return this.handleResponse<T>(response, path);
  }

  async post<T>(path: string, body: unknown, secured = true): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: this.getHeaders(secured),
      body: JSON.stringify(body),
    });
    return this.handleResponse<T>(response, path);
  }

  async put<T>(path: string, body: unknown, secured = true): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'PUT',
      headers: this.getHeaders(secured),
      body: JSON.stringify(body),
    });
    return this.handleResponse<T>(response, path);
  }

  async patch<T>(path: string, body: unknown, secured = true): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'PATCH',
      headers: this.getHeaders(secured),
      body: JSON.stringify(body),
    });
    return this.handleResponse<T>(response, path);
  }

  async del<T>(path: string, secured = true): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'DELETE',
      headers: this.getHeaders(secured),
    });
    return this.handleResponse<T>(response, path);
  }

  /** Multipart upload — do not set Content-Type (browser sets boundary). */
  async postForm<T>(path: string, formData: FormData, secured = true): Promise<T> {
    const headers: Record<string, string> = {
      Accept: 'application/json',
    };

    if (secured) {
      const token = getStoredToken();
      if (!token) {
        const apiError: ApiError = {
          status: 401,
          message: 'Please sign in again — auth token is missing',
        };
        throw apiError;
      }
      headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers,
      body: formData,
    });
    return this.handleResponse<T>(response, path);
  }
}

export const api = new ApiClient();
export default api;
